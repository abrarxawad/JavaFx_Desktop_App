package com.lifelink.dao;

import com.lifelink.database.DatabaseManager;
import com.lifelink.exception.DatabaseException;
import com.lifelink.model.Notification;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * DAO for the {@code notifications} table.
 *
 * <p><b>Package:</b> com.lifelink.dao
 */
public class NotificationDAO {

    private static final Logger logger = LoggerFactory.getLogger(NotificationDAO.class);
    private final DatabaseManager dbManager = DatabaseManager.getInstance();

    private static final String SQL_INSERT = """
        INSERT INTO notifications (user_id, title, message, type, read_status)
        VALUES (?,?,?,?,0)
        """;

    private static final String SQL_BY_USER = """
        SELECT * FROM notifications WHERE user_id = ?
        ORDER BY created_at DESC LIMIT 50
        """;

    private static final String SQL_UNREAD_COUNT =
        "SELECT COUNT(*) FROM notifications WHERE user_id = ? AND read_status = 0";

    private static final String SQL_MARK_ALL_READ =
        "UPDATE notifications SET read_status = 1 WHERE user_id = ?";

    private static final String SQL_MARK_READ =
        "UPDATE notifications SET read_status = 1 WHERE notification_id = ?";

    // ── Create ────────────────────────────────────────────────────────────────

    public int create(int userId, String title, String message, String type) {
        try (var ctx = dbManager.getConnectionWrapper()) {
            Connection conn = ctx.getConnection();
            try (PreparedStatement ps = conn.prepareStatement(SQL_INSERT,
                    Statement.RETURN_GENERATED_KEYS)) {
                ps.setInt(1, userId);
                ps.setString(2, title);
                ps.setString(3, message);
                ps.setString(4, type != null ? type : "GENERAL");
                ps.executeUpdate();
                try (ResultSet keys = ps.getGeneratedKeys()) {
                    if (keys.next()) return keys.getInt(1);
                }
            }
        } catch (SQLException e) {
            throw new DatabaseException("Failed to create notification: " + e.getMessage(), e);
        }
        return -1;
    }

    // ── Read ──────────────────────────────────────────────────────────────────

    public List<Notification> findByUser(int userId) {
        List<Notification> list = new ArrayList<>();
        try (var ctx = dbManager.getConnectionWrapper()) {
            Connection conn = ctx.getConnection();
            try (PreparedStatement ps = conn.prepareStatement(SQL_BY_USER)) {
                ps.setInt(1, userId);
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) list.add(mapRow(rs));
                }
            }
        } catch (SQLException e) {
            throw new DatabaseException("Error fetching notifications: " + e.getMessage(), e);
        }
        return list;
    }

    public int countUnread(int userId) {
        try (var ctx = dbManager.getConnectionWrapper()) {
            Connection conn = ctx.getConnection();
            try (PreparedStatement ps = conn.prepareStatement(SQL_UNREAD_COUNT)) {
                ps.setInt(1, userId);
                try (ResultSet rs = ps.executeQuery()) { return rs.next() ? rs.getInt(1) : 0; }
            }
        } catch (SQLException e) {
            throw new DatabaseException("Error counting notifications: " + e.getMessage(), e);
        }
    }

    // ── Update ────────────────────────────────────────────────────────────────

    public void markAllRead(int userId) {
        try (var ctx = dbManager.getConnectionWrapper()) {
            Connection conn = ctx.getConnection();
            try (PreparedStatement ps = conn.prepareStatement(SQL_MARK_ALL_READ)) {
                ps.setInt(1, userId);
                ps.executeUpdate();
            }
        } catch (SQLException e) {
            throw new DatabaseException("Error marking notifications read: " + e.getMessage(), e);
        }
    }

    public void markRead(int notifId) {
        try (var ctx = dbManager.getConnectionWrapper()) {
            Connection conn = ctx.getConnection();
            try (PreparedStatement ps = conn.prepareStatement(SQL_MARK_READ)) {
                ps.setInt(1, notifId);
                ps.executeUpdate();
            }
        } catch (SQLException e) {
            throw new DatabaseException("Error marking notification read: " + e.getMessage(), e);
        }
    }

    // ── Row mapping ───────────────────────────────────────────────────────────

    private Notification mapRow(ResultSet rs) throws SQLException {
        Notification n = new Notification();
        n.setNotificationId(rs.getInt("notification_id"));
        n.setUserId(rs.getInt("user_id"));
        n.setTitle(rs.getString("title"));
        n.setMessage(rs.getString("message"));
        n.setType(rs.getString("type"));
        n.setRead(rs.getInt("read_status") == 1);
        String ca = rs.getString("created_at");
        if (ca != null && !ca.isEmpty()) {
            try { n.setCreatedAt(LocalDateTime.parse(ca.replace(" ", "T"))); } catch (Exception ignored) {}
        }
        return n;
    }
}
