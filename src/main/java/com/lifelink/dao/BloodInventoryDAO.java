package com.lifelink.dao;

import com.lifelink.database.DatabaseManager;
import com.lifelink.exception.DatabaseException;
import com.lifelink.model.BloodGroup;
import com.lifelink.model.BloodInventory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * DAO for the {@code blood_inventory} table.
 *
 * <p><b>Package:</b> com.lifelink.dao
 */
public class BloodInventoryDAO {

    private static final Logger logger = LoggerFactory.getLogger(BloodInventoryDAO.class);
    private final DatabaseManager dbManager = DatabaseManager.getInstance();

    private static final String SQL_INSERT = """
        INSERT INTO blood_inventory
            (blood_bank_id, blood_group, quantity, collection_date, expiry_date, status)
        VALUES (?,?,?,?,?,?)
        """;

    private static final String SQL_FIND_BY_BANK = """
        SELECT bi.*, bb.name as bank_name
        FROM blood_inventory bi
        JOIN blood_banks bb ON bi.blood_bank_id = bb.blood_bank_id
        WHERE bi.blood_bank_id = ?
        ORDER BY bi.expiry_date ASC
        """;

    private static final String SQL_FIND_ALL = """
        SELECT bi.*, bb.name as bank_name
        FROM blood_inventory bi
        JOIN blood_banks bb ON bi.blood_bank_id = bb.blood_bank_id
        ORDER BY bi.expiry_date ASC
        """;

    private static final String SQL_UPDATE_QTY =
        "UPDATE blood_inventory SET quantity = ? WHERE inventory_id = ?";

    private static final String SQL_UPDATE_STATUS =
        "UPDATE blood_inventory SET status = ? WHERE inventory_id = ?";

    private static final String SQL_TOTAL_AVAILABLE_BY_BANK =
        "SELECT blood_group, SUM(quantity) as total FROM blood_inventory " +
        "WHERE blood_bank_id = ? AND status = 'AVAILABLE' GROUP BY blood_group";

    private static final String SQL_MARK_EXPIRED =
        "UPDATE blood_inventory SET status = 'EXPIRED' " +
        "WHERE expiry_date < date('now') AND status = 'AVAILABLE'";

    // ── Create ────────────────────────────────────────────────────────────────

    public int addInventory(BloodInventory inv) {
        try (var ctx = dbManager.getConnectionWrapper()) {
            Connection conn = ctx.getConnection();
            try (PreparedStatement ps = conn.prepareStatement(SQL_INSERT,
                    Statement.RETURN_GENERATED_KEYS)) {
                ps.setInt(1, inv.getBloodBankId());
                ps.setString(2, inv.getBloodGroup() != null ? inv.getBloodGroup().name() : "O_POSITIVE");
                ps.setInt(3, inv.getQuantity());
                ps.setString(4, inv.getCollectionDate() != null ? inv.getCollectionDate().toString() : LocalDate.now().toString());
                ps.setString(5, inv.getExpiryDate() != null ? inv.getExpiryDate().toString() : LocalDate.now().plusDays(42).toString());
                ps.setString(6, inv.getStatus() != null ? inv.getStatus() : BloodInventory.STATUS_AVAILABLE);
                ps.executeUpdate();

                try (ResultSet keys = ps.getGeneratedKeys()) {
                    if (keys.next()) {
                        int id = keys.getInt(1);
                        logger.info("Added inventory id={} for bank={}", id, inv.getBloodBankId());
                        return id;
                    }
                }
            }
        } catch (SQLException e) {
            throw new DatabaseException("Failed to add inventory: " + e.getMessage(), e);
        }
        throw new DatabaseException("Inventory insertion returned no key.");
    }

    // ── Read ──────────────────────────────────────────────────────────────────

    public List<BloodInventory> findByBank(int bloodBankId) {
        List<BloodInventory> list = new ArrayList<>();
        try (var ctx = dbManager.getConnectionWrapper()) {
            Connection conn = ctx.getConnection();
            try (PreparedStatement ps = conn.prepareStatement(SQL_FIND_BY_BANK)) {
                ps.setInt(1, bloodBankId);
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) list.add(mapRow(rs));
                }
            }
        } catch (SQLException e) {
            throw new DatabaseException("Error fetching inventory: " + e.getMessage(), e);
        }
        return list;
    }

    public List<BloodInventory> findAll() {
        List<BloodInventory> list = new ArrayList<>();
        try (var ctx = dbManager.getConnectionWrapper()) {
            Connection conn = ctx.getConnection();
            try (Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery(SQL_FIND_ALL)) {
                while (rs.next()) list.add(mapRow(rs));
            }
        } catch (SQLException e) {
            throw new DatabaseException("Error fetching all inventory: " + e.getMessage(), e);
        }
        return list;
    }

    /**
     * Returns a map of blood_group -> total quantity for a given bank.
     * Used to populate the inventory summary cards.
     */
    public java.util.Map<String, Integer> getTotalAvailableByBank(int bloodBankId) {
        java.util.Map<String, Integer> result = new java.util.LinkedHashMap<>();
        try (var ctx = dbManager.getConnectionWrapper()) {
            Connection conn = ctx.getConnection();
            try (PreparedStatement ps = conn.prepareStatement(SQL_TOTAL_AVAILABLE_BY_BANK)) {
                ps.setInt(1, bloodBankId);
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        result.put(rs.getString("blood_group"), rs.getInt("total"));
                    }
                }
            }
        } catch (SQLException e) {
            throw new DatabaseException("Error fetching inventory summary: " + e.getMessage(), e);
        }
        return result;
    }

    // ── Update ────────────────────────────────────────────────────────────────

    public void updateQuantity(int inventoryId, int newQuantity) {
        try (var ctx = dbManager.getConnectionWrapper()) {
            Connection conn = ctx.getConnection();
            try (PreparedStatement ps = conn.prepareStatement(SQL_UPDATE_QTY)) {
                ps.setInt(1, newQuantity);
                ps.setInt(2, inventoryId);
                ps.executeUpdate();
            }
        } catch (SQLException e) {
            throw new DatabaseException("Error updating inventory quantity: " + e.getMessage(), e);
        }
    }

    public void updateStatus(int inventoryId, String status) {
        try (var ctx = dbManager.getConnectionWrapper()) {
            Connection conn = ctx.getConnection();
            try (PreparedStatement ps = conn.prepareStatement(SQL_UPDATE_STATUS)) {
                ps.setString(1, status);
                ps.setInt(2, inventoryId);
                ps.executeUpdate();
            }
        } catch (SQLException e) {
            throw new DatabaseException("Error updating inventory status: " + e.getMessage(), e);
        }
    }

    /** Marks all expired blood batches as EXPIRED. */
    public int markExpired() {
        try (var ctx = dbManager.getConnectionWrapper()) {
            Connection conn = ctx.getConnection();
            try (Statement stmt = conn.createStatement()) {
                return stmt.executeUpdate(SQL_MARK_EXPIRED);
            }
        } catch (SQLException e) {
            throw new DatabaseException("Error marking expired inventory: " + e.getMessage(), e);
        }
    }

    // ── Row mapping ───────────────────────────────────────────────────────────

    private BloodInventory mapRow(ResultSet rs) throws SQLException {
        BloodInventory inv = new BloodInventory();
        inv.setInventoryId(rs.getInt("inventory_id"));
        inv.setBloodBankId(rs.getInt("blood_bank_id"));
        try { inv.setBloodBankName(rs.getString("bank_name")); } catch (SQLException ignored) {}
        String bg = rs.getString("blood_group");
        if (bg != null) {
            try { inv.setBloodGroup(BloodGroup.valueOf(bg)); } catch (IllegalArgumentException ignored) {}
        }
        inv.setQuantity(rs.getInt("quantity"));
        String cd = rs.getString("collection_date");
        if (cd != null && !cd.isEmpty()) try { inv.setCollectionDate(LocalDate.parse(cd.substring(0,10))); } catch (Exception ignored) {}
        String ed = rs.getString("expiry_date");
        if (ed != null && !ed.isEmpty()) try { inv.setExpiryDate(LocalDate.parse(ed.substring(0,10))); } catch (Exception ignored) {}
        inv.setStatus(rs.getString("status"));
        return inv;
    }
}
