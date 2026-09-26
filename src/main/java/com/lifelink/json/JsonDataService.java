package com.lifelink.json;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.lifelink.dao.BloodRequestDAO;
import com.lifelink.dao.DonorDAO;
import com.lifelink.dao.UserDAO;
import com.lifelink.model.BloodGroup;
import com.lifelink.model.BloodRequest;
import com.lifelink.model.Donor;
import com.lifelink.model.UserRole;
import com.lifelink.security.PasswordHasher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

public final class JsonDataService {
    private static final Logger logger = LoggerFactory.getLogger(JsonDataService.class);
    public static final Path EXPORT_PATH = Paths.get("data", "exports");
    public static final Path IMPORT_PATH = Paths.get("data", "imports");

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

    private JsonDataService() {
    }

    public static ObjectMapper getObjectMapper() {
        return OBJECT_MAPPER;
    }

    public static JsonAppConfig loadAppConfig() {
        try (InputStream is = JsonDataService.class.getResourceAsStream("/config/app-config.json")) {
            if (is == null) {
                JsonAppConfig defaultConfig = new JsonAppConfig();
                saveAppConfig(defaultConfig, Paths.get("src", "main", "resources", "config", "app-config.json"));
                return defaultConfig;
            }
            return OBJECT_MAPPER.readValue(is, JsonAppConfig.class);
        } catch (IOException e) {
            logger.error("Failed to load app-config.json: {}", e.getMessage(), e);
            return new JsonAppConfig();
        }
    }

    public static void saveAppConfig(JsonAppConfig config, Path targetPath) {
        try {
            Path parent = targetPath.getParent();
            if (parent != null) Files.createDirectories(parent);
            OBJECT_MAPPER.writerWithDefaultPrettyPrinter().writeValue(targetPath.toFile(), config);
            logger.info("Saved JSON config to {}", targetPath);
        } catch (IOException e) {
            logger.error("Failed to save app-config.json: {}", e.getMessage(), e);
        }
    }

    public static void exportDonorsToJson(Path path) {
        try {
            Path parent = path.getParent();
            if (parent != null) Files.createDirectories(parent);
            List<Donor> donors = new DonorDAO().findAll();
            OBJECT_MAPPER.writerWithDefaultPrettyPrinter().writeValue(path.toFile(), donors);
            logger.info("Exported {} donors to JSON at {}", donors.size(), path.toAbsolutePath());
        } catch (IOException e) {
            logger.error("JSON donor export failed: {}", e.getMessage(), e);
        }
    }

    public static void exportBloodRequestsToJson(Path path) {
        try {
            Path parent = path.getParent();
            if (parent != null) Files.createDirectories(parent);
            List<BloodRequest> requests = new BloodRequestDAO().findAll();
            OBJECT_MAPPER.writerWithDefaultPrettyPrinter().writeValue(path.toFile(), requests);
            logger.info("Exported {} blood requests to JSON at {}", requests.size(), path.toAbsolutePath());
        } catch (IOException e) {
            logger.error("JSON blood-request export failed: {}", e.getMessage(), e);
        }
    }

    public static List<Donor> importDonorsFromJson(Path path) {
        try {
            List<JsonDonorImportRecord> records = OBJECT_MAPPER.readValue(
                    path.toFile(),
                    new TypeReference<>() {}
            );
            List<Donor> imported = new ArrayList<>();
            UserDAO userDAO = new UserDAO();
            DonorDAO donorDAO = new DonorDAO();

            for (JsonDonorImportRecord record : records) {
                validateImportedDonor(record);
                String username = record.getUsername() == null || record.getUsername().isBlank()
                        ? record.getFirstName().toLowerCase() + "." + record.getLastName().toLowerCase()
                        : record.getUsername();
                String email = record.getEmail() == null || record.getEmail().isBlank()
                        ? username + "@lifelink.local"
                        : record.getEmail();

                String defaultPasswordHash = PasswordHasher.hash("ChangeMe123!");
                int userId = userDAO.createUser(username, email, defaultPasswordHash, UserRole.DONOR);
                Donor donor = new Donor();
                donor.setUserId(userId);
                donor.setFirstName(record.getFirstName());
                donor.setLastName(record.getLastName());
                donor.setBloodGroup(record.getBloodGroupEnum());
                donor.setDateOfBirth(record.getDateOfBirth());
                donor.setGender(record.getGender() == null ? "OTHER" : record.getGender());
                donor.setPhone(record.getPhone());
                donor.setAddress(record.getAddress());
                donor.setCity(record.getCity());
                donor.setLatitude(record.getLatitude());
                donor.setLongitude(record.getLongitude());
                donor.setAvailable(record.isAvailable());
                donor.setEligibilityStatus(record.getEligibilityStatus() == null ? "ELIGIBLE" : record.getEligibilityStatus());
                donor.setTotalDonations(record.getTotalDonations());
                donorDAO.createDonor(donor);
                imported.add(donor);
            }
            logger.info("Imported {} donors from JSON file {}", imported.size(), path.toAbsolutePath());
            return imported;
        } catch (IOException e) {
            logger.error("JSON donor import failed: {}", e.getMessage(), e);
            return List.of();
        }
    }

    public static void importDonorsToDatabase(Path path) {
        List<Donor> donors = importDonorsFromJson(path);
        if (!donors.isEmpty()) {
            logger.info("Bulk donor import completed with {} donor records.", donors.size());
        }
    }

    public static String serializeRequestToJson(JsonApiRequest request) throws JsonProcessingException {
        return OBJECT_MAPPER.writeValueAsString(request);
    }

    public static JsonApiRequest deserializeRequestFromJson(String json) throws JsonProcessingException {
        return OBJECT_MAPPER.readValue(json, JsonApiRequest.class);
    }

    public static String serializeResponseToJson(JsonApiResponse response) throws JsonProcessingException {
        return OBJECT_MAPPER.writeValueAsString(response);
    }

    public static JsonApiResponse deserializeResponseFromJson(String json) throws JsonProcessingException {
        return OBJECT_MAPPER.readValue(json, JsonApiResponse.class);
    }

    public static JsonApiResponse simulateHospitalApi(JsonApiRequest request) {
        try {
            String requestJson = serializeRequestToJson(request);
            logger.info("Mock hospital service request JSON: {}", requestJson);

            JsonApiResponse response = new JsonApiResponse();
            response.setStatus("READY");
            response.setMessage("Hospital request accepted by the mock blood-bank service.");
            response.setMatchedDonors(Math.max(1, request.getUnitsNeeded() / 2));
            response.setEtaMinutes(request.isUrgent() ? 20 : 45);

            String responseJson = serializeResponseToJson(response);
            logger.info("Mock hospital service response JSON: {}", responseJson);
            return deserializeResponseFromJson(responseJson);
        } catch (JsonProcessingException e) {
            logger.error("Hospital API simulation failed: {}", e.getMessage(), e);
            JsonApiResponse error = new JsonApiResponse();
            error.setStatus("ERROR");
            error.setMessage("Invalid JSON payload");
            return error;
        }
    }

    public static void runJsonDemo() {
        JsonAppConfig config = loadAppConfig();
        logger.info("Loaded app config: radius={} km, lowStockThreshold={}, notificationsEnabled={}",
                config.getEmergencyMatchingRadiusKm(),
                config.getLowStockThreshold(),
                config.isNotificationsEnabled());

        List<Donor> donors = new DonorDAO().findAll();
        if (!donors.isEmpty()) {
            String exportJson = toJsonString(donors);
            logger.info("Export demo JSON: {}", exportJson);
        }

        JsonApiRequest request = new JsonApiRequest();
        request.setHospitalId(101);
        request.setHospitalName("City General Hospital");
        request.setBloodGroup("O-");
        request.setUnitsNeeded(4);
        request.setUrgent(true);
        request.setRequestedBy("Emergency Ward");

        JsonApiResponse response = simulateHospitalApi(request);
        logger.info("API simulation result: status={}, matchedDonors={}, etaMinutes={}",
                response.getStatus(), response.getMatchedDonors(), response.getEtaMinutes());
    }

    public static String toJsonString(Object value) {
        try {
            return OBJECT_MAPPER.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            logger.error("Failed to serialize value to JSON: {}", e.getMessage(), e);
            return "{}";
        }
    }

    public static <T> T fromJsonString(String json, Class<T> type) throws JsonProcessingException {
        return OBJECT_MAPPER.readValue(json, type);
    }

    public static <T> List<T> fromJsonList(String json, TypeReference<List<T>> typeReference) throws JsonProcessingException {
        return OBJECT_MAPPER.readValue(json, typeReference);
    }

    private static void validateImportedDonor(JsonDonorImportRecord record) {
        if (record == null) {
            throw new IllegalArgumentException("Imported donor record cannot be null.");
        }
        if (record.getFirstName() == null || record.getFirstName().isBlank()) {
            throw new IllegalArgumentException("Imported donor is missing firstName.");
        }
        if (record.getLastName() == null || record.getLastName().isBlank()) {
            throw new IllegalArgumentException("Imported donor is missing lastName.");
        }
        if (record.getEmail() == null || record.getEmail().isBlank()) {
            throw new IllegalArgumentException("Imported donor is missing email.");
        }
        if (record.getBloodGroupEnum() == null) {
            throw new IllegalArgumentException("Imported donor has an invalid blood group.");
        }
        if (record.getDateOfBirth() == null) {
            throw new IllegalArgumentException("Imported donor is missing dateOfBirth.");
        }
    }
}
