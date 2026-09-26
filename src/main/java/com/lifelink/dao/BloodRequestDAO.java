package com.lifelink.dao;

import com.lifelink.database.DatabaseManager;
import com.lifelink.exception.DatabaseException;
import com.lifelink.model.BloodGroup;
import com.lifelink.model.BloodRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Data Access Object for the {@code blood_requests} table.
 *
 * <p><b>Package:</b> com.lifelink.dao
 * <p><b>Used by:</b> BloodRequestService, RecipientDashboardController,
 *                    HospitalDashboardController, AdminDashboardController
 */
public class BloodRequestDAO {

    private static final Logger logger = LoggerFactory.getLogger(BloodRequestDAO.class);
    private final DatabaseManager dbManager = DatabaseManager.getInstance();

    // ── SQL ───────────────────────────────────────────────────────────────────

    private static final String SQL_INSERT = """
        INSERT INTO blood_requests
            (requester_id, blood_group, quantity, hospital_id, hospital_name, requester_type, requester_location, requester_city, priority, status, notes)
        VALUES (?,?,?,?,?,?,?,?,?,?,?)
        """;

    private static final String SQL_FIND_ALL = """
        SELECT br.*, u.username as requester_name
        FROM blood_requests br
        JOIN users u ON br.requester_id = u.user_id
        ORDER BY br.request_id DESC
        """;

    private static final String SQL_FIND_BY_REQUESTER = """
        SELECT br.*, u.username as requester_name
        FROM blood_requests br
        JOIN users u ON br.requester_id = u.user_id
        WHERE br.requester_id = ?
        ORDER BY br.request_id DESC
        """;

    private static final String SQL_FIND_PENDING = """
        SELECT br.*, u.username as requester_name
        FROM blood_requests br
        JOIN users u ON br.requester_id = u.user_id
        WHERE br.status IN ('PENDING','MATCHING','PARTIALLY_FULFILLED','AWAITING_CONFIRMATION')
        ORDER BY
          CASE br.priority WHEN 'EMERGENCY' THEN 1 WHEN 'URGENT' THEN 2 ELSE 3 END,
          br.request_date ASC
        """;

    private static final String SQL_FIND_BY_BLOOD_GROUP = """
        SELECT br.*, u.username as requester_name
        FROM blood_requests br
        JOIN users u ON br.requester_id = u.user_id
        WHERE br.blood_group = ? AND br.status IN ('PENDING','MATCHING','AWAITING_CONFIRMATION')
        ORDER BY br.request_date DESC
        """;

    private static final String SQL_UPDATE_STATUS =
        "UPDATE blood_requests SET status = ? WHERE request_id = ?";

    private static final String SQL_COUNT_BY_STATUS =
        "SELECT COUNT(*) FROM blood_requests WHERE status = ?";

    private static final String SQL_COUNT_ALL =
        "SELECT COUNT(*) FROM blood_requests";

    // ── Create ────────────────────────────────────────────────────────────────

    public int create(BloodRequest req) {
        try (var ctx = dbManager.getConnectionWrapper()) {
            Connection conn = ctx.getConnection();
            try (PreparedStatement ps = conn.prepareStatement(SQL_INSERT,
                    Statement.RETURN_GENERATED_KEYS)) {
                ps.setInt(1, req.getRequesterId());
                ps.setString(2, req.getBloodGroup() != null ? req.getBloodGroup().name() : "O_POSITIVE");
                ps.setInt(3, req.getQuantity());
                if (req.getHospitalId() != null) ps.setInt(4, req.getHospitalId()); else ps.setNull(4, Types.INTEGER);
                ps.setString(5, req.getHospitalName() != null ? req.getHospitalName() : "");
                ps.setString(6, req.getRequesterType() != null ? req.getRequesterType() : "RECIPIENT");
                ps.setString(7, req.getRequesterLocation() != null ? req.getRequesterLocation() : "");
                ps.setString(8, req.getRequesterCity() != null ? req.getRequesterCity() : "");
                ps.setString(9, req.getPriority() != null ? req.getPriority() : "NORMAL");
                ps.setString(10, req.getStatus() != null ? req.getStatus() : "PENDING");
                ps.setString(11, req.getNotes());
                ps.executeUpdate();

                try (ResultSet keys = ps.getGeneratedKeys()) {
                    if (keys.next()) {
                        int id = keys.getInt(1);
                        logger.info("Created BloodRequest id={} by user_id={}", id, req.getRequesterId());
                        return id;
                    }
                }
            }
        } catch (SQLException e) {
            throw new DatabaseException("Failed to create blood request: " + e.getMessage(), e);
        }
        throw new DatabaseException("Blood request insertion returned no key.");
    }

    // ── Read ──────────────────────────────────────────────────────────────────

    public List<BloodRequest> findAll() {
        List<BloodRequest> list = new ArrayList<>();
        try (var ctx = dbManager.getConnectionWrapper()) {
            Connection conn = ctx.getConnection();
            try (Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery(SQL_FIND_ALL)) {
                while (rs.next()) list.add(mapRow(rs));
            }
        } catch (SQLException e) {
            throw new DatabaseException("Error fetching blood requests: " + e.getMessage(), e);
        }
        return list;
    }

    public List<BloodRequest> findByRequesterId(int requesterId) {
        List<BloodRequest> list = new ArrayList<>();
        try (var ctx = dbManager.getConnectionWrapper()) {
            Connection conn = ctx.getConnection();
            try (PreparedStatement ps = conn.prepareStatement(SQL_FIND_BY_REQUESTER)) {
                ps.setInt(1, requesterId);
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) list.add(mapRow(rs));
                }
            }
        } catch (SQLException e) {
            throw new DatabaseException("Error fetching requests by requester: " + e.getMessage(), e);
        }
        return list;
    }

    public List<BloodRequest> findPending() {
        List<BloodRequest> list = new ArrayList<>();
        try (var ctx = dbManager.getConnectionWrapper()) {
            Connection conn = ctx.getConnection();
            try (Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery(SQL_FIND_PENDING)) {
                while (rs.next()) list.add(mapRow(rs));
            }
        } catch (SQLException e) {
            throw new DatabaseException("Error fetching pending requests: " + e.getMessage(), e);
        }
        return list;
    }

    public List<BloodRequest> findByBloodGroup(String bloodGroup) {
        List<BloodRequest> list = new ArrayList<>();
        try (var ctx = dbManager.getConnectionWrapper()) {
            Connection conn = ctx.getConnection();
            try (PreparedStatement ps = conn.prepareStatement(SQL_FIND_BY_BLOOD_GROUP)) {
                ps.setString(1, bloodGroup);
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) list.add(mapRow(rs));
                }
            }
        } catch (SQLException e) {
            throw new DatabaseException("Error fetching requests by blood group: " + e.getMessage(), e);
        }
        return list;
    }

    public void updateStatus(int requestId, String newStatus) {
        try (var ctx = dbManager.getConnectionWrapper()) {
            Connection conn = ctx.getConnection();
            try (PreparedStatement ps = conn.prepareStatement(SQL_UPDATE_STATUS)) {
                ps.setString(1, newStatus);
                ps.setInt(2, requestId);
                ps.executeUpdate();
                logger.info("Updated BloodRequest id={} status to '{}'", requestId, newStatus);
            }
        } catch (SQLException e) {
            throw new DatabaseException("Error updating request status: " + e.getMessage(), e);
        }
    }

    // ── Stats ─────────────────────────────────────────────────────────────────

    public int countByStatus(String status) {
        try (var ctx = dbManager.getConnectionWrapper()) {
            Connection conn = ctx.getConnection();
            try (PreparedStatement ps = conn.prepareStatement(SQL_COUNT_BY_STATUS)) {
                ps.setString(1, status);
                try (ResultSet rs = ps.executeQuery()) { return rs.next() ? rs.getInt(1) : 0; }
            }
        } catch (SQLException e) {
            throw new DatabaseException("Error counting requests: " + e.getMessage(), e);
        }
    }

    public int countAll() {
        try (var ctx = dbManager.getConnectionWrapper()) {
            Connection conn = ctx.getConnection();
            try (Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery(SQL_COUNT_ALL)) {
                return rs.next() ? rs.getInt(1) : 0;
            }
        } catch (SQLException e) {
            throw new DatabaseException("Error counting all requests: " + e.getMessage(), e);
        }
    }

    // ── Row mapping ───────────────────────────────────────────────────────────

    private BloodRequest mapRow(ResultSet rs) throws SQLException {
        BloodRequest.Builder b = new BloodRequest.Builder()
            .requesterId(rs.getInt("requester_id"))
            .quantity(rs.getInt("quantity"))
            .priority(rs.getString("priority"))
            .status(rs.getString("status"))
            .hospitalName(rs.getString("hospital_name"))
            .requesterLocation(rs.getString("requester_location"))
            .requesterCity(rs.getString("requester_city"))
            .requesterType(rs.getString("requester_type"))
            .notes(rs.getString("notes"));

        // Blood group
        String bg = rs.getString("blood_group");
        if (bg != null) {
            try { b.bloodGroup(BloodGroup.valueOf(bg)); } catch (IllegalArgumentException ignored) {}
        }

        // requester name (joined)
        try { b.requesterName(rs.getString("requester_name")); } catch (SQLException ignored) {}

        // hospital
        int hid = rs.getInt("hospital_id");
        if (!rs.wasNull()) b.hospitalId(hid);

        // dates
        String rd = rs.getString("request_date");
        if (rd != null) {
            try { b.requestDate(java.time.LocalDateTime.parse(rd.replace(" ", "T"))); } catch (Exception ignored) {}
        }

        BloodRequest req = b.build();
        req.setRequestId(rs.getInt("request_id"));
        return req;
    }
}
