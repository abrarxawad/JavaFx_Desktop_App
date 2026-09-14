package com.lifelink.dao;

import com.lifelink.database.DatabaseManager;
import com.lifelink.exception.DatabaseException;
import com.lifelink.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.util.Optional;

/**
 * Data Access Object for the {@code users} table.
 *
 * <p><b>Design Pattern — DAO:</b> This class is the only place in the application
 * that writes raw SQL for the {@code users} table. No controller or service class
 * ever touches JDBC directly — they all call methods on this DAO instead.
 *
 * <p><b>SQL Injection Prevention:</b> Every query uses {@link PreparedStatement}.
 * User-supplied strings are passed as parameters, never concatenated into SQL strings.
 *
 * <p><b>Architecture Layer:</b>
 * <pre>
 *   Controller → Service → UserDAO → DatabaseManager → MySQL
 * </pre>
 *
 * <p><b>Package:</b> com.lifelink.dao
 * <p><b>Used by:</b> AuthService, AdminService
 */
public class UserDAO {

    private static final Logger logger = LoggerFactory.getLogger(UserDAO.class);
    private final DatabaseManager dbManager = DatabaseManager.getInstance();

    // ── SQL Constants ─────────────────────────────────────────────────────────

    private static final String SQL_INSERT =
        "INSERT INTO users (username, email, password_hash, role, status) " +
        "VALUES (?, ?, ?, ?, ?)";

    private static final String SQL_FIND_BY_USERNAME =
        "SELECT user_id, username, email, password_hash, role, status, created_at, updated_at " +
        "FROM users WHERE username = ?";

    private static final String SQL_FIND_BY_EMAIL =
        "SELECT user_id, username, email, password_hash, role, status, created_at, updated_at " +
        "FROM users WHERE email = ?";

    private static final String SQL_FIND_BY_ID =
        "SELECT user_id, username, email, password_hash, role, status, created_at, updated_at " +
        "FROM users WHERE user_id = ?";

    private static final String SQL_UPDATE_STATUS =
        "UPDATE users SET status = ? WHERE user_id = ?";

    private static final String SQL_UPDATE_PASSWORD =
        "UPDATE users SET password_hash = ? WHERE user_id = ?";

    private static final String SQL_COUNT_BY_ROLE =
        "SELECT COUNT(*) FROM users WHERE role = ? AND status = 'ACTIVE'";

    private static final String SQL_EXISTS_USERNAME =
        "SELECT 1 FROM users WHERE username = ?";

    private static final String SQL_EXISTS_EMAIL =
        "SELECT 1 FROM users WHERE email = ?";

    // ── Create ────────────────────────────────────────────────────────────────

    /**
     * Inserts a new user into the database and returns the generated user_id.
     *
     * <p>The password stored here MUST already be hashed by {@link com.lifelink.security.PasswordHasher}.
     *
     * @param username     unique username
     * @param email        unique email
     * @param passwordHash BCrypt hash of the password
     * @param role         user role
     * @return the auto-generated {@code user_id}
     * @throws DatabaseException if insertion fails (e.g. duplicate username/email)
     */
    public int createUser(String username, String email, String passwordHash, UserRole role) {
        try (var ctx = dbManager.getConnectionWrapper()) {
            Connection conn = ctx.getConnection();
            try (PreparedStatement ps = conn.prepareStatement(SQL_INSERT,
                                         Statement.RETURN_GENERATED_KEYS)) {
                ps.setString(1, username);
                ps.setString(2, email);
                ps.setString(3, passwordHash);
                ps.setString(4, role.name());
                ps.setString(5, "ACTIVE");
                ps.executeUpdate();

                try (ResultSet keys = ps.getGeneratedKeys()) {
                    if (keys.next()) {
                        int id = keys.getInt(1);
                        logger.info("Created user '{}' with id={}, role={}", username, id, role);
                        return id;
                    }
                }
            }
        } catch (SQLException e) {
            logger.error("Failed to create user '{}': {}", username, e.getMessage());
            if (e.getErrorCode() == 1062) { // MySQL duplicate entry
                throw new DatabaseException(
                    "Duplicate username or email: " + username,
                    "An account with this username or email already exists.", e
                );
            }
            throw new DatabaseException("Failed to create user: " + e.getMessage(), e);
        }
        throw new DatabaseException("User insertion returned no generated key.");
    }

    // ── Read ──────────────────────────────────────────────────────────────────

    /**
     * Finds a user by username (used during login).
     *
     * @param username the username to search
     * @return {@link Optional} containing the User, or empty if not found
     */
    public Optional<User> findByUsername(String username) {
        try (var ctx = dbManager.getConnectionWrapper()) {
            Connection conn = ctx.getConnection();
            try (PreparedStatement ps = conn.prepareStatement(SQL_FIND_BY_USERNAME)) {
                ps.setString(1, username);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        return Optional.of(mapRow(rs));
                    }
                }
            }
        } catch (SQLException e) {
            logger.error("Error finding user by username '{}': {}", username, e.getMessage());
            throw new DatabaseException("Error querying user: " + e.getMessage(), e);
        }
        return Optional.empty();
    }

    /**
     * Finds a user by their primary key.
     */
    public Optional<User> findById(int userId) {
        try (var ctx = dbManager.getConnectionWrapper()) {
            Connection conn = ctx.getConnection();
            try (PreparedStatement ps = conn.prepareStatement(SQL_FIND_BY_ID)) {
                ps.setInt(1, userId);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        return Optional.of(mapRow(rs));
                    }
                }
            }
        } catch (SQLException e) {
            throw new DatabaseException("Error finding user by id: " + e.getMessage(), e);
        }
        return Optional.empty();
    }

    // ── Update ────────────────────────────────────────────────────────────────

    public void updateStatus(int userId, String status) {
        try (var ctx = dbManager.getConnectionWrapper()) {
            Connection conn = ctx.getConnection();
            try (PreparedStatement ps = conn.prepareStatement(SQL_UPDATE_STATUS)) {
                ps.setString(1, status);
                ps.setInt(2, userId);
                ps.executeUpdate();
                logger.info("Updated user {} status to '{}'", userId, status);
            }
        } catch (SQLException e) {
            throw new DatabaseException("Failed to update user status: " + e.getMessage(), e);
        }
    }

    public void updatePasswordHash(int userId, String newHash) {
        try (var ctx = dbManager.getConnectionWrapper()) {
            Connection conn = ctx.getConnection();
            try (PreparedStatement ps = conn.prepareStatement(SQL_UPDATE_PASSWORD)) {
                ps.setString(1, newHash);
                ps.setInt(2, userId);
                ps.executeUpdate();
                logger.info("Password updated for user {}", userId);
            }
        } catch (SQLException e) {
            throw new DatabaseException("Failed to update password: " + e.getMessage(), e);
        }
    }

    // ── Existence checks ──────────────────────────────────────────────────────

    public boolean existsByUsername(String username) {
        try (var ctx = dbManager.getConnectionWrapper()) {
            Connection conn = ctx.getConnection();
            try (PreparedStatement ps = conn.prepareStatement(SQL_EXISTS_USERNAME)) {
                ps.setString(1, username);
                try (ResultSet rs = ps.executeQuery()) {
                    return rs.next();
                }
            }
        } catch (SQLException e) {
            throw new DatabaseException("Error checking username: " + e.getMessage(), e);
        }
    }

    public boolean existsByEmail(String email) {
        try (var ctx = dbManager.getConnectionWrapper()) {
            Connection conn = ctx.getConnection();
            try (PreparedStatement ps = conn.prepareStatement(SQL_EXISTS_EMAIL)) {
                ps.setString(1, email);
                try (ResultSet rs = ps.executeQuery()) {
                    return rs.next();
                }
            }
        } catch (SQLException e) {
            throw new DatabaseException("Error checking email: " + e.getMessage(), e);
        }
    }

    // ── Stats ─────────────────────────────────────────────────────────────────

    public int countActiveByRole(UserRole role) {
        try (var ctx = dbManager.getConnectionWrapper()) {
            Connection conn = ctx.getConnection();
            try (PreparedStatement ps = conn.prepareStatement(SQL_COUNT_BY_ROLE)) {
                ps.setString(1, role.name());
                try (ResultSet rs = ps.executeQuery()) {
                    return rs.next() ? rs.getInt(1) : 0;
                }
            }
        } catch (SQLException e) {
            throw new DatabaseException("Error counting users: " + e.getMessage(), e);
        }
    }

    // ── Row mapping ───────────────────────────────────────────────────────────

    /**
     * Maps a single ResultSet row to a {@link User} subclass based on role.
     * This is the OOP Polymorphism point — the correct subclass is instantiated
     * based on the role column.
     */
    private User mapRow(ResultSet rs) throws SQLException {
        UserRole role = UserRole.valueOf(rs.getString("role"));

        User user = switch (role) {
            case DONOR      -> new Donor();
            case RECIPIENT  -> new Recipient();
            case ADMIN      -> new Admin();
            default         -> new Admin(); // BLOOD_BANK, HOSPITAL — use Admin shell
        };

        user.setUserId(rs.getInt("user_id"));
        user.setUsername(rs.getString("username"));
        user.setEmail(rs.getString("email"));
        user.setPasswordHash(rs.getString("password_hash"));
        user.setRole(role);
        user.setStatus(rs.getString("status"));

        Timestamp created = rs.getTimestamp("created_at");
        Timestamp updated = rs.getTimestamp("updated_at");
        if (created != null) user.setCreatedAt(created.toLocalDateTime());
        if (updated != null) user.setUpdatedAt(updated.toLocalDateTime());

        return user;
    }
}
