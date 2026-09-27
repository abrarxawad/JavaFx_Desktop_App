package com.lifelink.database;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lifelink.dao.DonorDAO;
import com.lifelink.json.JsonDataService;
import com.lifelink.model.BloodGroup;
import com.lifelink.model.Donor;
import com.lifelink.model.UserRole;
import com.lifelink.security.PasswordHasher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import java.util.Objects;

public final class DemoDataBootstrap {
    private static final Logger logger = LoggerFactory.getLogger(DemoDataBootstrap.class);
    private static final ObjectMapper OBJECT_MAPPER = JsonDataService.getObjectMapper();

    private DemoDataBootstrap() {
    }

    public static void ensureDatasetLoaded() {
        if (!Boolean.parseBoolean(System.getProperty("lifelink.demo.data.enabled", "true"))) {
            logger.info("Synthetic demo dataset bootstrap is disabled via lifelink.demo.data.enabled=false.");
            return;
        }

        try (var ctx = DatabaseManager.getInstance().getConnectionWrapper()) {
            Connection conn = ctx.getConnection();
            if (hasDonors(conn) && hasRecipients(conn)) {
                logger.info("Synthetic demo dataset already exists in SQLite — skipping bootstrap.");
                return;
            }

            Path datasetPath = Paths.get(System.getProperty("user.dir"), "lifelink_demo_dataset_200_donors_200_recipients.json");
            Path alternatePath = Paths.get(System.getProperty("user.dir"), "lifelink_Dataset.json");
            
            if (!Files.exists(datasetPath)) {
                if (Files.exists(alternatePath)) {
                    datasetPath = alternatePath;
                } else {
                    logger.warn("Synthetic demo dataset JSON not found. "
                            + "The application will start without pre-loaded demo data. "
                            + "Copy lifelink_Dataset.json to the project root to enable it.");
                    return;
                }
            }

            JsonNode rootNode = OBJECT_MAPPER.readTree(Files.newInputStream(datasetPath));
            int donorsAdded     = loadDonors(conn, rootNode.path("donors"));
            int recipientsAdded = loadRecipients(conn, rootNode.path("recipients"));
            logger.info("DEMO DATA — NOT REAL DONOR/RECIPIENT INFORMATION. "
                      + "Loaded {} synthetic demo donors and {} synthetic demo recipients.",
                      donorsAdded, recipientsAdded);

        } catch (IOException | SQLException e) {
            // Log clearly but do NOT crash the application — demo data is optional.
            logger.warn("Failed to load LifeLink demo dataset (non-fatal): {}", e.getMessage());
            logger.debug("Demo dataset bootstrap exception details:", e);
        }
    }

    private static boolean hasDonors(Connection conn) throws SQLException {
        try (Statement stmt = conn.createStatement(); ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM donors")) {
            return rs.next() && rs.getInt(1) > 0;
        }
    }

    private static boolean hasRecipients(Connection conn) throws SQLException {
        try (Statement stmt = conn.createStatement(); ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM recipients")) {
            return rs.next() && rs.getInt(1) > 0;
        }
    }

    private static int loadDonors(Connection conn, JsonNode donorsNode) throws SQLException {
        if (donorsNode == null || !donorsNode.isArray()) {
            return 0;
        }

        DonorDAO donorDAO = new DonorDAO();
        com.lifelink.dao.UserDAO userDAO = new com.lifelink.dao.UserDAO();
        int inserted = 0;
        int skipped  = 0;

        for (JsonNode donorNode : donorsNode) {
            if (donorNode == null || donorNode.isNull()) {
                continue;
            }

            try {
                String[] nameParts = extractNameParts(donorNode.path("name").asText());
                String firstName   = nameParts[0];
                String lastName    = nameParts[1];
                String email       = donorNode.path("email").asText(null);
                String username    = "demo.donor." + donorNode.path("id").asInt(inserted + 1);

                if (email == null || email.isBlank()) {
                    email = username + "@lifelink.test";
                }

                int userId = userDAO.createUser(username, email,
                        PasswordHasher.hash("ChangeMe123!"), UserRole.DONOR);

                Donor donor = new Donor();
                donor.setUserId(userId);
                donor.setFirstName(firstName);
                donor.setLastName(lastName);
                donor.setBloodGroup(BloodGroup.fromLabel(donorNode.path("bloodGroup").asText("O+")));
                donor.setDateOfBirth(LocalDate.now().minusYears(
                        Math.max(18, donorNode.path("age").asInt(30))));
                String gender = donorNode.path("gender").asText("OTHER").toUpperCase();
                donor.setGender(gender.isBlank() ? "OTHER" : gender);
                donor.setPhone(donorNode.path("phone").asText(""));
                donor.setAddress(donorNode.path("address").asText(""));
                donor.setCity(donorNode.path("city").asText(""));
                donor.setLatitude(donorNode.hasNonNull("latitude")
                        ? donorNode.get("latitude").asDouble() : null);
                donor.setLongitude(donorNode.hasNonNull("longitude")
                        ? donorNode.get("longitude").asDouble() : null);
                donor.setAvailable(donorNode.path("availability").asBoolean(true));
                donor.setLastDonationDate(parseDate(donorNode.path("lastDonationDate").asText(null)));
                donor.refreshEligibilityStatus();
                donor.setTotalDonations(0);
                donorDAO.createDonor(donor);
                inserted++;

            } catch (Exception e) {
                // Skip individual bad records without stopping the whole import
                skipped++;
                logger.debug("Skipped demo donor record (index {}): {}", inserted + skipped, e.getMessage());
            }
        }

        if (skipped > 0) {
            logger.warn("Skipped {} donor records during demo data import.", skipped);
        }
        return inserted;
    }

    private static int loadRecipients(Connection conn, JsonNode recipientsNode) throws SQLException {
        if (recipientsNode == null || !recipientsNode.isArray()) {
            return 0;
        }

        com.lifelink.dao.UserDAO userDAO = new com.lifelink.dao.UserDAO();
        int inserted = 0;
        int skipped  = 0;

        for (JsonNode recipientNode : recipientsNode) {
            if (recipientNode == null || recipientNode.isNull()) {
                continue;
            }

            try {
                String[] nameParts = extractNameParts(recipientNode.path("name").asText());
                String username    = "demo.recipient." + recipientNode.path("id").asInt(inserted + 1);
                String email       = recipientNode.path("email").asText(null);
                if (email == null || email.isBlank()) {
                    email = username + "@lifelink.test";
                }

                int userId = userDAO.createUser(username, email,
                        PasswordHasher.hash("ChangeMe123!"), UserRole.RECIPIENT);

                String sql = "INSERT INTO recipients "
                           + "(user_id, first_name, last_name, blood_group, phone, address, city, latitude, longitude) "
                           + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";
                try (PreparedStatement ps = conn.prepareStatement(sql)) {
                    ps.setInt(1, userId);
                    ps.setString(2, nameParts[0]);
                    ps.setString(3, nameParts[1]);
                    ps.setString(4, BloodGroup.fromLabel(
                            recipientNode.path("bloodGroup").asText("O+")).name());
                    ps.setString(5, recipientNode.path("phone").asText(""));
                    ps.setString(6, recipientNode.path("address").asText(""));
                    ps.setString(7, recipientNode.path("city").asText(""));
                    if (recipientNode.hasNonNull("latitude"))
                        ps.setDouble(8, recipientNode.get("latitude").asDouble());
                    else
                        ps.setNull(8, java.sql.Types.REAL);
                    if (recipientNode.hasNonNull("longitude"))
                        ps.setDouble(9, recipientNode.get("longitude").asDouble());
                    else
                        ps.setNull(9, java.sql.Types.REAL);
                    ps.executeUpdate();
                    inserted++;
                }

            } catch (Exception e) {
                // Skip individual bad records without stopping the whole import
                skipped++;
                logger.debug("Skipped demo recipient record (index {}): {}",
                        inserted + skipped, e.getMessage());
            }
        }

        if (skipped > 0) {
            logger.warn("Skipped {} recipient records during demo data import.", skipped);
        }
        return inserted;
    }

    private static String[] extractNameParts(String name) {
        if (name == null || name.isBlank()) {
            return new String[] {"Demo", "User"};
        }
        String[] parts = name.trim().split("\\s+");
        if (parts.length == 1) {
            return new String[] {parts[0], "User"};
        }
        String first = parts[0];
        String last = String.join(" ", java.util.Arrays.copyOfRange(parts, 1, parts.length));
        return new String[] {first, last};
    }

    private static LocalDate parseDate(String text) {
        if (text == null || text.isBlank()) {
            return null;
        }
        try {
            return LocalDate.parse(text);
        } catch (Exception ignored) {
            return null;
        }
    }
}
