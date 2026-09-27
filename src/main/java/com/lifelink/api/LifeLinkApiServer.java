package com.lifelink.api;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lifelink.dao.BloodInventoryDAO;
import com.lifelink.dao.BloodRequestDAO;
import com.lifelink.dao.DonorDAO;
import com.lifelink.database.DatabaseManager;
import com.lifelink.json.JsonDataService;
import com.lifelink.model.BloodGroup;
import com.lifelink.model.BloodInventory;
import com.lifelink.model.BloodRequest;
import com.lifelink.model.Donor;
import com.lifelink.util.HaversineCalculator;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

/**
 * Embedded REST API server for LifeLink using the JDK's built-in
 * {@link HttpServer}. No external web framework is required.
 *
 * <h3>Endpoints</h3>
 * <pre>
 *   GET  /api/health
 *   GET  /api/donors                     — all donors; ?bloodGroup=O%2B to filter
 *   GET  /api/donors/compatible          — compatible donors; ?bloodGroup=&latitude=&longitude=&radius=
 *   GET  /api/recipients                 — all recipients; ?bloodGroup=B%2B to filter
 *   GET  /api/blood-requests             — all blood requests
 *   GET  /api/blood-requests/urgent      — only EMERGENCY / PENDING requests
 *   GET  /api/inventory                  — current blood inventory
 *   GET  /api/hospitals                  — registered hospitals
 *   GET  /api/blood-banks                — registered blood banks
 * </pre>
 *
 * <p>All responses are JSON via Jackson. The server listens on port 8080 by default.
 *
 * <p><b>Package:</b> com.lifelink.api
 */
public class LifeLinkApiServer {

    private static final Logger logger = LoggerFactory.getLogger(LifeLinkApiServer.class);
    private static final ObjectMapper MAPPER = JsonDataService.getObjectMapper();
    private static final int DEFAULT_PORT = 8080;
    private static volatile LifeLinkApiServer instance;

    private final HttpServer httpServer;

    // ── Constructor ───────────────────────────────────────────────────────────

    private LifeLinkApiServer(int port) throws IOException {
        this.httpServer = HttpServer.create(new InetSocketAddress(port), 0);
        // Use a thread pool so that concurrent API requests don't block each other
        this.httpServer.setExecutor(Executors.newFixedThreadPool(4));
        configureRoutes();
    }

    // ── Lifecycle ─────────────────────────────────────────────────────────────

    public static LifeLinkApiServer start() {
        return start(DEFAULT_PORT);
    }

    public static LifeLinkApiServer start(int port) {
        if (instance != null) {
            logger.info("LifeLink REST API is already running on port {}.", instance.getPort());
            return instance;
        }
        synchronized (LifeLinkApiServer.class) {
            if (instance != null) {
                return instance;
            }
            try {
                LifeLinkApiServer server = new LifeLinkApiServer(port);
                server.httpServer.start();
                instance = server;
                logger.info("LifeLink REST API started on http://localhost:{}", server.getPort());
                return server;
            } catch (IOException e) {
                // If port is in use (e.g. previous instance not shut down), log and continue
                logger.warn("Unable to start LifeLink REST API on port {}: {}", port, e.getMessage());
                if (e.getMessage() != null && e.getMessage().contains("Address already in use")) {
                    logger.info("REST API port {} is already bound — another instance may be running. Continuing without embedded server.", port);
                    return null;
                }
                throw new IllegalStateException("Unable to start LifeLink REST API on port " + port, e);
            }
        }
    }

    public static void stopServer() {
        LifeLinkApiServer current = instance;
        if (current != null) {
            current.stop();
        }
    }

    public void stop() {
        if (httpServer != null) {
            httpServer.stop(0);
            instance = null;
            logger.info("LifeLink REST API stopped.");
        }
    }

    public int getPort() {
        return httpServer.getAddress().getPort();
    }

    // ── Routes ────────────────────────────────────────────────────────────────

    private void configureRoutes() {
        // Health check
        httpServer.createContext("/api/health",               this::handleHealth);

        // Donor endpoints
        // Note: /api/donors/compatible must be registered BEFORE /api/donors
        // because HttpServer matches on prefix and longer prefix wins only if
        // the context was created first.
        httpServer.createContext("/api/donors/compatible",    this::handleCompatibleDonors);
        httpServer.createContext("/api/donors",               this::handleDonors);

        // Recipient endpoints
        httpServer.createContext("/api/recipients",           this::handleRecipients);

        // Blood request endpoints
        httpServer.createContext("/api/blood-requests/urgent", this::handleUrgentRequests);
        httpServer.createContext("/api/blood-requests",        this::handleBloodRequests);

        // Inventory / blood-banks
        httpServer.createContext("/api/inventory",             this::handleInventory);
        httpServer.createContext("/api/blood-banks",           this::handleBloodBanks);

        // Hospital / nearby facilities (from database)
        httpServer.createContext("/api/hospitals",             this::handleHospitals);
    }

    // ── Handlers ──────────────────────────────────────────────────────────────

    private void handleHealth(HttpExchange exchange) throws IOException {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("status", "UP");
        payload.put("service", "LifeLink API");
        payload.put("version", "1.0.0");
        payload.put("database", "SQLite");
        payload.put("timestamp", Instant.now().toString());
        sendJson(exchange, 200, payload);
    }

    /**
     * GET /api/donors               — all donors
     * GET /api/donors?bloodGroup=O+ — filtered by blood group
     */
    private void handleDonors(HttpExchange exchange) throws IOException {
        if (!"GET".equalsIgnoreCase(exchange.getRequestMethod())) {
            sendJson(exchange, 405, Map.of("error", "Method not allowed. Use GET."));
            return;
        }

        Map<String, String> params = parseQuery(exchange.getRequestURI().getRawQuery());
        String bloodGroupParam = params.get("bloodGroup");

        try {
            List<Donor> donors = new DonorDAO().findAll();

            // Apply blood-group filter when requested
            if (bloodGroupParam != null && !bloodGroupParam.isBlank()) {
                try {
                    BloodGroup filter = BloodGroup.fromLabel(bloodGroupParam);
                    donors = donors.stream()
                            .filter(d -> filter.equals(d.getBloodGroup()))
                            .collect(Collectors.toList());
                } catch (IllegalArgumentException ex) {
                    sendJson(exchange, 400, Map.of("error", "Invalid blood group: " + bloodGroupParam));
                    return;
                }
            }

            Map<String, Object> payload = new LinkedHashMap<>();
            payload.put("count", donors.size());
            if (bloodGroupParam != null && !bloodGroupParam.isBlank()) {
                payload.put("bloodGroupFilter", bloodGroupParam);
            }
            payload.put("items", toSafeDonorList(donors, null, null));
            sendJson(exchange, 200, payload);

        } catch (Exception ex) {
            logger.error("Failed to load donors via REST API", ex);
            sendJson(exchange, 500, Map.of("error", "Failed to load donors: " + ex.getMessage()));
        }
    }

    /**
     * GET /api/donors/compatible?bloodGroup=O+
     * GET /api/donors/compatible?bloodGroup=AB+&latitude=22.84&longitude=89.54&radius=10
     */
    private void handleCompatibleDonors(HttpExchange exchange) throws IOException {
        if (!"GET".equalsIgnoreCase(exchange.getRequestMethod())) {
            sendJson(exchange, 405, Map.of("error", "Method not allowed. Use GET."));
            return;
        }

        Map<String, String> params = parseQuery(exchange.getRequestURI().getRawQuery());
        String bloodGroupText = params.get("bloodGroup");
        String latText        = params.get("latitude");
        String lonText        = params.get("longitude");
        String radiusText     = params.get("radius");

        if (bloodGroupText == null || bloodGroupText.isBlank()) {
            sendJson(exchange, 400, Map.of("error", "bloodGroup query parameter is required."));
            return;
        }

        BloodGroup targetGroup;
        try {
            targetGroup = BloodGroup.fromLabel(bloodGroupText);
        } catch (IllegalArgumentException ex) {
            sendJson(exchange, 400, Map.of("error", "Invalid blood group: " + bloodGroupText));
            return;
        }

        Double latitude  = parseDoubleOrNull(latText);
        Double longitude = parseDoubleOrNull(lonText);
        double radiusKm  = parseDouble(radiusText, 25.0);

        // Validate coordinates if provided
        if (latitude != null && longitude != null) {
            if (!Double.isFinite(latitude) || latitude < -90 || latitude > 90) {
                sendJson(exchange, 400, Map.of("error", "Invalid latitude value."));
                return;
            }
            if (!Double.isFinite(longitude) || longitude < -180 || longitude > 180) {
                sendJson(exchange, 400, Map.of("error", "Invalid longitude value."));
                return;
            }
        }

        try {
            List<Donor> matches = new DonorDAO().findCompatibleDonors(targetGroup, latitude, longitude, latitude != null ? radiusKm : null);

            Map<String, Object> payload = new LinkedHashMap<>();
            payload.put("recipientBloodGroup", targetGroup.getLabel());
            payload.put("compatibleDonorGroups",
                    BloodGroup.getCompatibleDonorGroups(targetGroup).stream()
                              .map(BloodGroup::getLabel)
                              .collect(Collectors.toList()));
            if (latitude != null) {
                payload.put("latitude",  latitude);
                payload.put("longitude", longitude);
                payload.put("radiusKm",  radiusKm);
            }
            payload.put("count", matches.size());
            payload.put("items", toSafeDonorList(matches, latitude, longitude));
            sendJson(exchange, 200, payload);

        } catch (Exception ex) {
            logger.error("Compatible donor lookup failed", ex);
            sendJson(exchange, 503, Map.of("error", "Compatible donor lookup is temporarily unavailable: " + ex.getMessage()));
        }
    }

    /**
     * GET /api/recipients               — all recipients
     * GET /api/recipients?bloodGroup=B+ — filtered by blood group
     */
    private void handleRecipients(HttpExchange exchange) throws IOException {
        if (!"GET".equalsIgnoreCase(exchange.getRequestMethod())) {
            sendJson(exchange, 405, Map.of("error", "Method not allowed. Use GET."));
            return;
        }

        Map<String, String> params     = parseQuery(exchange.getRequestURI().getRawQuery());
        String bloodGroupParam          = params.get("bloodGroup");
        String bloodGroupFilter         = null;

        if (bloodGroupParam != null && !bloodGroupParam.isBlank()) {
            try {
                bloodGroupFilter = BloodGroup.fromLabel(bloodGroupParam).name();
            } catch (IllegalArgumentException ex) {
                sendJson(exchange, 400, Map.of("error", "Invalid blood group: " + bloodGroupParam));
                return;
            }
        }

        final String filterBg = bloodGroupFilter;
        List<Map<String, Object>> rows = new ArrayList<>();
        String sql = "SELECT r.recipient_id, r.user_id, r.first_name, r.last_name, r.blood_group, "
                   + "r.phone, r.address, r.city, r.latitude, r.longitude, "
                   + "u.username, u.email "
                   + "FROM recipients r "
                   + "JOIN users u ON r.user_id = u.user_id "
                   + (filterBg != null ? "WHERE r.blood_group = '" + filterBg.replace("'", "''") + "' " : "")
                   + "ORDER BY r.recipient_id DESC";

        try (var ctx = DatabaseManager.getInstance().getConnectionWrapper();
             Connection conn = ctx.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                Map<String, Object> row = new LinkedHashMap<>();
                row.put("recipientId", rs.getInt("recipient_id"));
                row.put("userId",      rs.getInt("user_id"));
                row.put("firstName",   rs.getString("first_name"));
                row.put("lastName",    rs.getString("last_name"));
                String bg = rs.getString("blood_group");
                try { row.put("bloodGroup", bg != null ? BloodGroup.valueOf(bg).getLabel() : null); }
                catch (IllegalArgumentException ignored) { row.put("bloodGroup", bg); }
                row.put("phone",     rs.getString("phone"));
                row.put("address",   rs.getString("address"));
                row.put("city",      rs.getString("city"));
                Object lat = rs.getObject("latitude");
                Object lon = rs.getObject("longitude");
                row.put("latitude",  lat);
                row.put("longitude", lon);
                rows.add(row);
            }
        } catch (Exception ex) {
            logger.error("Unable to load recipients via REST API.", ex);
            sendJson(exchange, 500, Map.of("error", "Unable to load recipients from SQLite."));
            return;
        }

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("count", rows.size());
        if (bloodGroupParam != null && !bloodGroupParam.isBlank()) {
            payload.put("bloodGroupFilter", bloodGroupParam);
        }
        payload.put("items", rows);
        sendJson(exchange, 200, payload);
    }

    /**
     * GET /api/blood-requests — all blood requests
     */
    private void handleBloodRequests(HttpExchange exchange) throws IOException {
        if (!"GET".equalsIgnoreCase(exchange.getRequestMethod())) {
            sendJson(exchange, 405, Map.of("error", "Method not allowed. Use GET."));
            return;
        }

        try {
            List<BloodRequest> requests = new BloodRequestDAO().findAll();
            Map<String, Object> payload = new LinkedHashMap<>();
            payload.put("count", requests.size());
            payload.put("items", requests);
            sendJson(exchange, 200, payload);
        } catch (Exception ex) {
            logger.error("Failed to load blood requests via REST API", ex);
            sendJson(exchange, 500, Map.of("error", "Failed to load blood requests: " + ex.getMessage()));
        }
    }

    /**
     * GET /api/blood-requests/urgent — EMERGENCY or PENDING blood requests only
     */
    private void handleUrgentRequests(HttpExchange exchange) throws IOException {
        if (!"GET".equalsIgnoreCase(exchange.getRequestMethod())) {
            sendJson(exchange, 405, Map.of("error", "Method not allowed. Use GET."));
            return;
        }

        try {
            List<BloodRequest> all = new BloodRequestDAO().findAll();
            List<BloodRequest> urgent = all.stream()
                    .filter(r -> BloodRequest.PRIORITY_EMERGENCY.equalsIgnoreCase(r.getPriority())
                              || BloodRequest.STATUS_PENDING.equalsIgnoreCase(r.getStatus()))
                    .collect(Collectors.toList());

            Map<String, Object> payload = new LinkedHashMap<>();
            payload.put("count", urgent.size());
            payload.put("filter", "EMERGENCY_OR_PENDING");
            payload.put("items", urgent);
            sendJson(exchange, 200, payload);
        } catch (Exception ex) {
            logger.error("Failed to load urgent blood requests", ex);
            sendJson(exchange, 500, Map.of("error", "Failed to load urgent requests: " + ex.getMessage()));
        }
    }

    /**
     * GET /api/inventory — current blood inventory across all banks
     */
    private void handleInventory(HttpExchange exchange) throws IOException {
        if (!"GET".equalsIgnoreCase(exchange.getRequestMethod())) {
            sendJson(exchange, 405, Map.of("error", "Method not allowed. Use GET."));
            return;
        }

        try {
            List<BloodInventory> inventory = new BloodInventoryDAO().findAll();
            Map<String, Object> payload = new LinkedHashMap<>();
            payload.put("count", inventory.size());
            payload.put("items", inventory);
            sendJson(exchange, 200, payload);
        } catch (Exception ex) {
            logger.error("Failed to load inventory via REST API", ex);
            sendJson(exchange, 500, Map.of("error", "Failed to load inventory: " + ex.getMessage()));
        }
    }

    /**
     * GET /api/hospitals — hospitals registered in the LifeLink database
     */
    private void handleHospitals(HttpExchange exchange) throws IOException {
        if (!"GET".equalsIgnoreCase(exchange.getRequestMethod())) {
            sendJson(exchange, 405, Map.of("error", "Method not allowed. Use GET."));
            return;
        }
        sendFacilityList(exchange, "hospitals",
                "SELECT hospital_id, user_id, name, address, city, latitude, longitude, phone, email, status FROM hospitals ORDER BY hospital_id DESC");
    }

    /**
     * GET /api/blood-banks — blood banks registered in the LifeLink database
     */
    private void handleBloodBanks(HttpExchange exchange) throws IOException {
        if (!"GET".equalsIgnoreCase(exchange.getRequestMethod())) {
            sendJson(exchange, 405, Map.of("error", "Method not allowed. Use GET."));
            return;
        }
        sendFacilityList(exchange, "blood_banks",
                "SELECT blood_bank_id AS facility_id, user_id, name, address, city, latitude, longitude, phone, email, status FROM blood_banks ORDER BY blood_bank_id DESC");
    }

    private void sendFacilityList(HttpExchange exchange, String tableName, String sql) throws IOException {
        List<Map<String, Object>> rows = new ArrayList<>();
        try (var ctx = DatabaseManager.getInstance().getConnectionWrapper();
             Connection conn = ctx.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                Map<String, Object> row = new LinkedHashMap<>();
                // facility_id could be hospital_id or blood_bank_id aliased
                try { row.put("id",      rs.getObject("facility_id")); } catch (Exception ignored) {}
                try { row.put("userId",  rs.getObject("user_id")); }    catch (Exception ignored) {}
                row.put("name",      rs.getString("name"));
                row.put("address",   rs.getString("address"));
                row.put("city",      rs.getString("city"));
                row.put("latitude",  rs.getObject("latitude"));
                row.put("longitude", rs.getObject("longitude"));
                row.put("phone",     rs.getString("phone"));
                row.put("email",     rs.getObject("email"));
                row.put("status",    rs.getString("status"));
                rows.add(row);
            }
        } catch (Exception ex) {
            logger.error("Unable to load {} via REST API: {}", tableName, ex.getMessage(), ex);
            sendJson(exchange, 500, Map.of("error", "Unable to load " + tableName + " from database."));
            return;
        }

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("count", rows.size());
        payload.put("items", rows);
        sendJson(exchange, 200, payload);
    }

    // ── Helper: Convert donors to safe REST-response maps ────────────────────

    /**
     * Converts a list of {@link Donor} objects to REST-safe maps that include
     * {@code distanceKm} when coordinates are supplied, and omit internal
     * fields (password hash, etc.) that should never be exposed via the API.
     */
    private List<Map<String, Object>> toSafeDonorList(List<Donor> donors,
                                                       Double searchLat,
                                                       Double searchLon) {
        List<Map<String, Object>> result = new ArrayList<>();
        for (Donor d : donors) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("donorId",           d.getDonorId());
            m.put("firstName",         d.getFirstName());
            m.put("lastName",          d.getLastName());
            m.put("displayName",       d.getDisplayName());
            m.put("bloodGroup",        d.getBloodGroup() != null ? d.getBloodGroup().getLabel() : null);
            m.put("gender",            d.getGender());
            m.put("city",              d.getCity());
            m.put("phone",             d.getPhone());
            m.put("available",         d.isAvailable());
            m.put("eligibilityStatus", d.getEligibilityStatus());
            m.put("totalDonations",    d.getTotalDonations());
            m.put("lastDonationDate",  d.getLastDonationDate() != null ? d.getLastDonationDate().toString() : null);
            m.put("latitude",          d.getLatitude());
            m.put("longitude",         d.getLongitude());

            // Include computed distance when a search origin is supplied
            if (searchLat != null && searchLon != null && d.hasLocation()) {
                double dist = HaversineCalculator.calculateDistanceKm(
                        searchLat, searchLon, d.getLatitude(), d.getLongitude());
                m.put("distanceKm", Math.round(dist * 100.0) / 100.0);
            }

            result.add(m);
        }
        return result;
    }

    // ── JSON utilities ────────────────────────────────────────────────────────

    private static void sendJson(HttpExchange exchange, int statusCode, Object payload) throws IOException {
        byte[] responseBody;
        try {
            responseBody = MAPPER.writerWithDefaultPrettyPrinter().writeValueAsBytes(payload);
        } catch (JsonProcessingException e) {
            responseBody = "{\"error\":\"Failed to serialize API response\"}".getBytes(StandardCharsets.UTF_8);
        }

        exchange.getResponseHeaders().set("Content-Type", "application/json; charset=UTF-8");
        exchange.getResponseHeaders().set("Access-Control-Allow-Origin",  "*");
        exchange.getResponseHeaders().set("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS");
        exchange.getResponseHeaders().set("Access-Control-Allow-Headers", "Content-Type, Accept");
        exchange.sendResponseHeaders(statusCode, responseBody.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(responseBody);
        }
    }

    // ── Query parameter parsing ───────────────────────────────────────────────

    private static Map<String, String> parseQuery(String rawQuery) {
        Map<String, String> params = new LinkedHashMap<>();
        if (rawQuery == null || rawQuery.isBlank()) {
            return params;
        }
        for (String part : rawQuery.split("&")) {
            if (part.isBlank()) continue;
            String[] kv = part.split("=", 2);
            String key   = URLDecoder.decode(kv[0], StandardCharsets.UTF_8);
            String value = kv.length > 1 ? URLDecoder.decode(kv[1], StandardCharsets.UTF_8) : "";
            params.put(key, value);
        }
        return params;
    }

    private static double parseDouble(String value, double defaultValue) {
        if (value == null || value.isBlank()) return defaultValue;
        try { return Double.parseDouble(value); } catch (Exception ex) { return defaultValue; }
    }

    private static Double parseDoubleOrNull(String value) {
        if (value == null || value.isBlank()) return null;
        try { return Double.parseDouble(value); } catch (Exception ex) { return null; }
    }

    private static String readBody(HttpExchange exchange) throws IOException {
        try (InputStream is = exchange.getRequestBody()) {
            return new String(is.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
}
