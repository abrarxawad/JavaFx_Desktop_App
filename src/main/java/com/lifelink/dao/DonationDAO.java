package com.lifelink.dao;

import com.lifelink.database.DatabaseManager;
import com.lifelink.exception.DatabaseException;
import com.lifelink.model.BloodGroup;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * DAO for the {@code donations} table.
 *
 * <p><b>Package:</b> com.lifelink.dao
 */
public class DonationDAO {

    private static final Logger logger = LoggerFactory.getLogger(DonationDAO.class);
    private final DatabaseManager dbManager = DatabaseManager.getInstance();

    // ── Simple DTO for a donation record (inner class) ─────────────────────────

    public static class DonationRecord {
        public int donationId, donorId, quantityMl;
        public Integer bloodBankId, requestId;
        public String bloodGroup, status, notes, bankName;
        public LocalDate donationDate;

        @Override
        public String toString() {
            return "DonationRecord{id=" + donationId + ", group=" + bloodGroup
                    + ", date=" + donationDate + ", status=" + status + "}";
        }
    }

    private static final String SQL_INSERT = """
        INSERT INTO donations (donor_id, blood_bank_id, request_id, donation_date, blood_group, quantity_ml, status, notes)
        VALUES (?,?,?,?,?,?,?,?)
        """;

    private static final String SQL_BY_DONOR = """
        SELECT d.*, bb.name as bank_name
        FROM donations d
        LEFT JOIN blood_banks bb ON d.blood_bank_id = bb.blood_bank_id
        WHERE d.donor_id = ?
        ORDER BY d.donation_date DESC
        """;

    private static final String SQL_COUNT_ALL = "SELECT COUNT(*) FROM donations";

    private static final String SQL_COUNT_BY_DONOR = "SELECT COUNT(*) FROM donations WHERE donor_id = ?";

    // ── Create ────────────────────────────────────────────────────────────────

    public int recordDonation(DonationRecord rec) {
        try (var ctx = dbManager.getConnectionWrapper()) {
            Connection conn = ctx.getConnection();
            try (PreparedStatement ps = conn.prepareStatement(SQL_INSERT,
                    Statement.RETURN_GENERATED_KEYS)) {
                ps.setInt(1, rec.donorId);
                if (rec.bloodBankId != null) ps.setInt(2, rec.bloodBankId); else ps.setNull(2, Types.INTEGER);
                if (rec.requestId != null) ps.setInt(3, rec.requestId); else ps.setNull(3, Types.INTEGER);
                ps.setString(4, rec.donationDate != null ? rec.donationDate.toString() : LocalDate.now().toString());
                ps.setString(5, rec.bloodGroup != null ? rec.bloodGroup : "O_POSITIVE");
                ps.setInt(6, rec.quantityMl > 0 ? rec.quantityMl : 450);
                ps.setString(7, rec.status != null ? rec.status : "COMPLETED");
                ps.setString(8, rec.notes);
                ps.executeUpdate();

                try (ResultSet keys = ps.getGeneratedKeys()) {
                    if (keys.next()) {
                        int id = keys.getInt(1);
                        logger.info("Recorded donation id={} for donor_id={}", id, rec.donorId);
                        return id;
                    }
                }
            }
        } catch (SQLException e) {
            throw new DatabaseException("Failed to record donation: " + e.getMessage(), e);
        }
        throw new DatabaseException("Donation insertion returned no key.");
    }

    // ── Read ──────────────────────────────────────────────────────────────────

    public List<DonationRecord> findByDonor(int donorId) {
        List<DonationRecord> list = new ArrayList<>();
        try (var ctx = dbManager.getConnectionWrapper()) {
            Connection conn = ctx.getConnection();
            try (PreparedStatement ps = conn.prepareStatement(SQL_BY_DONOR)) {
                ps.setInt(1, donorId);
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) list.add(mapRow(rs));
                }
            }
        } catch (SQLException e) {
            throw new DatabaseException("Error fetching donations: " + e.getMessage(), e);
        }
        return list;
    }

    public int countAll() {
        try (var ctx = dbManager.getConnectionWrapper()) {
            Connection conn = ctx.getConnection();
            try (Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery(SQL_COUNT_ALL)) {
                return rs.next() ? rs.getInt(1) : 0;
            }
        } catch (SQLException e) {
            throw new DatabaseException("Error counting donations: " + e.getMessage(), e);
        }
    }

    public int countByDonor(int donorId) {
        try (var ctx = dbManager.getConnectionWrapper()) {
            Connection conn = ctx.getConnection();
            try (PreparedStatement ps = conn.prepareStatement(SQL_COUNT_BY_DONOR)) {
                ps.setInt(1, donorId);
                try (ResultSet rs = ps.executeQuery()) { return rs.next() ? rs.getInt(1) : 0; }
            }
        } catch (SQLException e) {
            throw new DatabaseException("Error counting donor donations: " + e.getMessage(), e);
        }
    }

    // ── Row mapping ───────────────────────────────────────────────────────────

    private DonationRecord mapRow(ResultSet rs) throws SQLException {
        DonationRecord rec = new DonationRecord();
        rec.donationId = rs.getInt("donation_id");
        rec.donorId = rs.getInt("donor_id");
        int bbid = rs.getInt("blood_bank_id"); if (!rs.wasNull()) rec.bloodBankId = bbid;
        int reqid = rs.getInt("request_id"); if (!rs.wasNull()) rec.requestId = reqid;
        String d = rs.getString("donation_date");
        if (d != null && !d.isEmpty()) try { rec.donationDate = LocalDate.parse(d.substring(0,10)); } catch (Exception ignored) {}
        rec.bloodGroup = rs.getString("blood_group");
        rec.quantityMl = rs.getInt("quantity_ml");
        rec.status = rs.getString("status");
        rec.notes = rs.getString("notes");
        try { rec.bankName = rs.getString("bank_name"); } catch (SQLException ignored) {}
        return rec;
    }
}
