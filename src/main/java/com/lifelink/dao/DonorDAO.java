package com.lifelink.dao;

import com.lifelink.database.DatabaseManager;
import com.lifelink.exception.DatabaseException;
import com.lifelink.model.BloodGroup;
import com.lifelink.model.Donor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Data Access Object for the {@code donors} table.
 *
 * <p><b>Design Pattern — DAO:</b> All SQL for the donors table lives here.
 * No controller or service touches JDBC directly.
 *
 * <p><b>Package:</b> com.lifelink.dao
 * <p><b>Used by:</b> DonorService, DonorDashboardController, MatchingEngine
 */
public class DonorDAO {

    private static final Logger logger = LoggerFactory.getLogger(DonorDAO.class);
    private final DatabaseManager dbManager = DatabaseManager.getInstance();

    // ── SQL Constants ─────────────────────────────────────────────────────────

    private static final String SQL_INSERT = """
        INSERT INTO donors (user_id, first_name, last_name, blood_group,
            date_of_birth, gender, phone, address, city, latitude, longitude,
            availability, eligibility_status, total_donations)
        VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?)
        """;

    private static final String SQL_FIND_BY_USER_ID = """
        SELECT * FROM donors WHERE user_id = ?
        """;

    private static final String SQL_FIND_BY_ID = """
        SELECT * FROM donors WHERE donor_id = ?
        """;

    private static final String SQL_FIND_ALL = """
        SELECT d.*, u.username, u.email FROM donors d
        JOIN users u ON d.user_id = u.user_id
        ORDER BY d.donor_id DESC
        """;

    private static final String SQL_UPDATE = """
        UPDATE donors SET first_name=?, last_name=?, phone=?, address=?,
            city=?, availability=?, eligibility_status=?, last_donation_date=?,
            total_donations=?, latitude=?, longitude=?
        WHERE donor_id=?
        """;

    private static final String SQL_COUNT = "SELECT COUNT(*) FROM donors";

    private static final String SQL_EXISTS_BY_USER = "SELECT 1 FROM donors WHERE user_id = ?";

    private static final String SQL_FIND_ELIGIBLE_BY_BLOOD_GROUP = """
        SELECT d.* FROM donors d
        WHERE d.blood_group = ? AND d.availability = 1
          AND d.eligibility_status = 'ELIGIBLE'
        ORDER BY d.total_donations DESC
        """;

    // ── Create ────────────────────────────────────────────────────────────────

    /**
     * Creates a donor profile row linked to an existing user_id.
     *
     * @return the generated donor_id
     */
    public int createDonor(Donor donor) {
        try (var ctx = dbManager.getConnectionWrapper()) {
            Connection conn = ctx.getConnection();
            try (PreparedStatement ps = conn.prepareStatement(SQL_INSERT,
                    Statement.RETURN_GENERATED_KEYS)) {
                ps.setInt(1, donor.getUserId());
                ps.setString(2, donor.getFirstName());
                ps.setString(3, donor.getLastName());
                ps.setString(4, donor.getBloodGroup() != null ? donor.getBloodGroup().name() : "O_POSITIVE");
                ps.setString(5, donor.getDateOfBirth() != null ? donor.getDateOfBirth().toString() : LocalDate.now().minusYears(20).toString());
                ps.setString(6, donor.getGender() != null ? donor.getGender() : "OTHER");
                ps.setString(7, donor.getPhone() != null ? donor.getPhone() : "");
                ps.setString(8, donor.getAddress() != null ? donor.getAddress() : "");
                ps.setString(9, donor.getCity() != null ? donor.getCity() : "");
                if (donor.getLatitude() != null) ps.setDouble(10, donor.getLatitude()); else ps.setNull(10, Types.REAL);
                if (donor.getLongitude() != null) ps.setDouble(11, donor.getLongitude()); else ps.setNull(11, Types.REAL);
                ps.setInt(12, donor.isAvailable() ? 1 : 0);
                ps.setString(13, donor.getEligibilityStatus() != null ? donor.getEligibilityStatus() : "ELIGIBLE");
                ps.setInt(14, donor.getTotalDonations());
                ps.executeUpdate();

                try (ResultSet keys = ps.getGeneratedKeys()) {
                    if (keys.next()) {
                        int id = keys.getInt(1);
                        logger.info("Created donor profile id={} for user_id={}", id, donor.getUserId());
                        return id;
                    }
                }
            }
        } catch (SQLException e) {
            logger.error("Failed to create donor: {}", e.getMessage());
            throw new DatabaseException("Failed to create donor profile: " + e.getMessage(), e);
        }
        throw new DatabaseException("Donor insertion returned no key.");
    }

    // ── Read ──────────────────────────────────────────────────────────────────

    public Optional<Donor> findByUserId(int userId) {
        try (var ctx = dbManager.getConnectionWrapper()) {
            Connection conn = ctx.getConnection();
            try (PreparedStatement ps = conn.prepareStatement(SQL_FIND_BY_USER_ID)) {
                ps.setInt(1, userId);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) return Optional.of(mapRow(rs));
                }
            }
        } catch (SQLException e) {
            throw new DatabaseException("Error finding donor: " + e.getMessage(), e);
        }
        return Optional.empty();
    }

    public Optional<Donor> findById(int donorId) {
        try (var ctx = dbManager.getConnectionWrapper()) {
            Connection conn = ctx.getConnection();
            try (PreparedStatement ps = conn.prepareStatement(SQL_FIND_BY_ID)) {
                ps.setInt(1, donorId);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) return Optional.of(mapRow(rs));
                }
            }
        } catch (SQLException e) {
            throw new DatabaseException("Error finding donor by id: " + e.getMessage(), e);
        }
        return Optional.empty();
    }

    public List<Donor> findAll() {
        List<Donor> list = new ArrayList<>();
        try (var ctx = dbManager.getConnectionWrapper()) {
            Connection conn = ctx.getConnection();
            try (Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery(SQL_FIND_ALL)) {
                while (rs.next()) list.add(mapRow(rs));
            }
        } catch (SQLException e) {
            throw new DatabaseException("Error fetching all donors: " + e.getMessage(), e);
        }
        return list;
    }

    public List<Donor> findEligibleByBloodGroup(String bloodGroup) {
        List<Donor> list = new ArrayList<>();
        try (var ctx = dbManager.getConnectionWrapper()) {
            Connection conn = ctx.getConnection();
            try (PreparedStatement ps = conn.prepareStatement(SQL_FIND_ELIGIBLE_BY_BLOOD_GROUP)) {
                ps.setString(1, bloodGroup);
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) list.add(mapRow(rs));
                }
            }
        } catch (SQLException e) {
            throw new DatabaseException("Error finding eligible donors: " + e.getMessage(), e);
        }
        return list;
    }

    public boolean existsByUserId(int userId) {
        try (var ctx = dbManager.getConnectionWrapper()) {
            Connection conn = ctx.getConnection();
            try (PreparedStatement ps = conn.prepareStatement(SQL_EXISTS_BY_USER)) {
                ps.setInt(1, userId);
                try (ResultSet rs = ps.executeQuery()) { return rs.next(); }
            }
        } catch (SQLException e) {
            throw new DatabaseException("Error checking donor existence: " + e.getMessage(), e);
        }
    }

    public int countAll() {
        try (var ctx = dbManager.getConnectionWrapper()) {
            Connection conn = ctx.getConnection();
            try (Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery(SQL_COUNT)) {
                return rs.next() ? rs.getInt(1) : 0;
            }
        } catch (SQLException e) {
            throw new DatabaseException("Error counting donors: " + e.getMessage(), e);
        }
    }

    // ── Update ────────────────────────────────────────────────────────────────

    public void update(Donor donor) {
        try (var ctx = dbManager.getConnectionWrapper()) {
            Connection conn = ctx.getConnection();
            try (PreparedStatement ps = conn.prepareStatement(SQL_UPDATE)) {
                ps.setString(1, donor.getFirstName());
                ps.setString(2, donor.getLastName());
                ps.setString(3, donor.getPhone());
                ps.setString(4, donor.getAddress());
                ps.setString(5, donor.getCity());
                ps.setInt(6, donor.isAvailable() ? 1 : 0);
                ps.setString(7, donor.getEligibilityStatus());
                ps.setString(8, donor.getLastDonationDate() != null ? donor.getLastDonationDate().toString() : null);
                ps.setInt(9, donor.getTotalDonations());
                if (donor.getLatitude() != null) ps.setDouble(10, donor.getLatitude()); else ps.setNull(10, Types.REAL);
                if (donor.getLongitude() != null) ps.setDouble(11, donor.getLongitude()); else ps.setNull(11, Types.REAL);
                ps.setInt(12, donor.getDonorId());
                ps.executeUpdate();
                logger.info("Updated donor id={}", donor.getDonorId());
            }
        } catch (SQLException e) {
            throw new DatabaseException("Error updating donor: " + e.getMessage(), e);
        }
    }

    // ── Row mapping ───────────────────────────────────────────────────────────

    private Donor mapRow(ResultSet rs) throws SQLException {
        Donor d = new Donor();
        d.setDonorId(rs.getInt("donor_id"));
        d.setUserId(rs.getInt("user_id"));
        d.setFirstName(rs.getString("first_name"));
        d.setLastName(rs.getString("last_name"));
        String bg = rs.getString("blood_group");
        if (bg != null) {
            try { d.setBloodGroup(BloodGroup.valueOf(bg)); }
            catch (IllegalArgumentException ignored) {}
        }
        String dob = rs.getString("date_of_birth");
        if (dob != null && !dob.isEmpty()) {
            try { d.setDateOfBirth(LocalDate.parse(dob.substring(0, 10))); }
            catch (Exception ignored) {}
        }
        d.setGender(rs.getString("gender"));
        d.setPhone(rs.getString("phone"));
        d.setAddress(rs.getString("address"));
        d.setCity(rs.getString("city"));
        double lat = rs.getDouble("latitude"); if (!rs.wasNull()) d.setLatitude(lat);
        double lon = rs.getDouble("longitude"); if (!rs.wasNull()) d.setLongitude(lon);
        String ldd = rs.getString("last_donation_date");
        if (ldd != null && !ldd.isEmpty()) {
            try { d.setLastDonationDate(LocalDate.parse(ldd.substring(0, 10))); }
            catch (Exception ignored) {}
        }
        d.setAvailable(rs.getInt("availability") == 1);
        d.setEligibilityStatus(rs.getString("eligibility_status"));
        d.setTotalDonations(rs.getInt("total_donations"));
        return d;
    }
}
