package com.lifelink.database;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.Statement;

/**
 * Initializes the SQLite database by creating all tables if they do not exist.
 *
 * <p>Tables created:
 * <ol>
 *   <li>users</li>
 *   <li>donors</li>
 *   <li>recipients</li>
 *   <li>blood_banks</li>
 *   <li>hospitals</li>
 *   <li>blood_inventory</li>
 *   <li>blood_requests</li>
 *   <li>donations</li>
 *   <li>notifications</li>
 *   <li>audit_logs</li>
 * </ol>
 *
 * <p><b>Package:</b> com.lifelink.database
 */
public class DatabaseInitializer {

    private static final Logger logger = LoggerFactory.getLogger(DatabaseInitializer.class);

    public static void initialize() {
        try (var ctx = DatabaseManager.getInstance().getConnectionWrapper()) {
            Connection conn = ctx.getConnection();
            try (Statement stmt = conn.createStatement()) {

                // Enable FK enforcement for SQLite
                stmt.execute("PRAGMA foreign_keys = ON;");
                stmt.execute("PRAGMA journal_mode = WAL;");

                // ── 1. users ──────────────────────────────────────────────────
                stmt.execute("""
                    CREATE TABLE IF NOT EXISTS users (
                        user_id       INTEGER PRIMARY KEY AUTOINCREMENT,
                        username      TEXT UNIQUE NOT NULL,
                        email         TEXT UNIQUE NOT NULL,
                        password_hash TEXT NOT NULL,
                        role          TEXT NOT NULL,
                        status        TEXT NOT NULL DEFAULT 'ACTIVE',
                        created_at    TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                        updated_at    TIMESTAMP DEFAULT CURRENT_TIMESTAMP
                    );
                    """);

                // ── 2. donors ─────────────────────────────────────────────────
                stmt.execute("""
                    CREATE TABLE IF NOT EXISTS donors (
                        donor_id           INTEGER PRIMARY KEY AUTOINCREMENT,
                        user_id            INTEGER NOT NULL UNIQUE,
                        first_name         TEXT NOT NULL,
                        last_name          TEXT NOT NULL,
                        blood_group        TEXT NOT NULL,
                        date_of_birth      TEXT NOT NULL,
                        gender             TEXT NOT NULL,
                        phone              TEXT NOT NULL,
                        address            TEXT NOT NULL DEFAULT '',
                        city               TEXT NOT NULL DEFAULT '',
                        latitude           REAL,
                        longitude          REAL,
                        last_donation_date TEXT,
                        availability       INTEGER NOT NULL DEFAULT 1,
                        eligibility_status TEXT NOT NULL DEFAULT 'ELIGIBLE',
                        total_donations    INTEGER NOT NULL DEFAULT 0,
                        FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE
                    );
                    """);

                // ── 3. recipients ─────────────────────────────────────────────
                stmt.execute("""
                    CREATE TABLE IF NOT EXISTS recipients (
                        recipient_id INTEGER PRIMARY KEY AUTOINCREMENT,
                        user_id      INTEGER NOT NULL UNIQUE,
                        first_name   TEXT NOT NULL,
                        last_name    TEXT NOT NULL,
                        blood_group  TEXT NOT NULL,
                        phone        TEXT NOT NULL,
                        address      TEXT NOT NULL DEFAULT '',
                        city         TEXT NOT NULL DEFAULT '',
                        latitude     REAL,
                        longitude    REAL,
                        FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE
                    );
                    """);

                // ── 4. blood_banks ────────────────────────────────────────────
                stmt.execute("""
                    CREATE TABLE IF NOT EXISTS blood_banks (
                        blood_bank_id INTEGER PRIMARY KEY AUTOINCREMENT,
                        user_id       INTEGER,
                        name          TEXT NOT NULL,
                        address       TEXT NOT NULL DEFAULT '',
                        city          TEXT NOT NULL DEFAULT '',
                        latitude      REAL,
                        longitude     REAL,
                        phone         TEXT NOT NULL DEFAULT '',
                        email         TEXT,
                        status        TEXT NOT NULL DEFAULT 'ACTIVE',
                        created_at    TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                        FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE SET NULL
                    );
                    """);

                // ── 5. hospitals ──────────────────────────────────────────────
                stmt.execute("""
                    CREATE TABLE IF NOT EXISTS hospitals (
                        hospital_id INTEGER PRIMARY KEY AUTOINCREMENT,
                        user_id     INTEGER,
                        name        TEXT NOT NULL,
                        address     TEXT NOT NULL DEFAULT '',
                        city        TEXT NOT NULL DEFAULT '',
                        latitude    REAL,
                        longitude   REAL,
                        phone       TEXT NOT NULL DEFAULT '',
                        email       TEXT,
                        status      TEXT NOT NULL DEFAULT 'ACTIVE',
                        created_at  TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                        FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE SET NULL
                    );
                    """);

                // ── 6. blood_inventory ────────────────────────────────────────
                stmt.execute("""
                    CREATE TABLE IF NOT EXISTS blood_inventory (
                        inventory_id    INTEGER PRIMARY KEY AUTOINCREMENT,
                        blood_bank_id   INTEGER NOT NULL,
                        blood_group     TEXT NOT NULL,
                        quantity        INTEGER NOT NULL DEFAULT 0,
                        collection_date TEXT NOT NULL,
                        expiry_date     TEXT NOT NULL,
                        status          TEXT NOT NULL DEFAULT 'AVAILABLE',
                        created_at      TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                        updated_at      TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                        FOREIGN KEY (blood_bank_id) REFERENCES blood_banks(blood_bank_id) ON DELETE CASCADE
                    );
                    """);

                // ── 7. blood_requests ─────────────────────────────────────────
                stmt.execute("""
                    CREATE TABLE IF NOT EXISTS blood_requests (
                        request_id    INTEGER PRIMARY KEY AUTOINCREMENT,
                        requester_id  INTEGER NOT NULL,
                        blood_group   TEXT NOT NULL,
                        quantity      INTEGER NOT NULL DEFAULT 1,
                        hospital_id   INTEGER,
                        priority      TEXT NOT NULL DEFAULT 'NORMAL',
                        status        TEXT NOT NULL DEFAULT 'PENDING',
                        request_date  TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                        required_date TIMESTAMP,
                        notes         TEXT,
                        updated_at    TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                        FOREIGN KEY (requester_id) REFERENCES users(user_id) ON DELETE CASCADE,
                        FOREIGN KEY (hospital_id) REFERENCES hospitals(hospital_id) ON DELETE SET NULL
                    );
                    """);

                // ── 8. donations ──────────────────────────────────────────────
                stmt.execute("""
                    CREATE TABLE IF NOT EXISTS donations (
                        donation_id   INTEGER PRIMARY KEY AUTOINCREMENT,
                        donor_id      INTEGER NOT NULL,
                        blood_bank_id INTEGER,
                        request_id    INTEGER,
                        donation_date TEXT NOT NULL,
                        blood_group   TEXT NOT NULL,
                        quantity_ml   INTEGER NOT NULL DEFAULT 450,
                        status        TEXT NOT NULL DEFAULT 'COMPLETED',
                        notes         TEXT,
                        created_at    TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                        FOREIGN KEY (donor_id) REFERENCES donors(donor_id) ON DELETE CASCADE,
                        FOREIGN KEY (blood_bank_id) REFERENCES blood_banks(blood_bank_id) ON DELETE SET NULL,
                        FOREIGN KEY (request_id) REFERENCES blood_requests(request_id) ON DELETE SET NULL
                    );
                    """);

                // ── 9. notifications ──────────────────────────────────────────
                stmt.execute("""
                    CREATE TABLE IF NOT EXISTS notifications (
                        notification_id INTEGER PRIMARY KEY AUTOINCREMENT,
                        user_id         INTEGER NOT NULL,
                        title           TEXT NOT NULL,
                        message         TEXT NOT NULL,
                        type            TEXT NOT NULL DEFAULT 'GENERAL',
                        read_status     INTEGER NOT NULL DEFAULT 0,
                        created_at      TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                        FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE
                    );
                    """);

                // ── 10. audit_logs ────────────────────────────────────────────
                stmt.execute("""
                    CREATE TABLE IF NOT EXISTS audit_logs (
                        log_id      INTEGER PRIMARY KEY AUTOINCREMENT,
                        user_id     INTEGER,
                        action      TEXT NOT NULL,
                        entity_type TEXT,
                        entity_id   INTEGER,
                        description TEXT,
                        ip_address  TEXT,
                        timestamp   TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                        FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE SET NULL
                    );
                    """);

                // ── Seed: default admin ───────────────────────────────────────
                // BCrypt hash of "Admin@1234" (cost=12)
                stmt.execute("""
                    INSERT OR IGNORE INTO users
                        (username, email, password_hash, role, status)
                    VALUES (
                        'admin',
                        'admin@lifelink.local',
                        '$2a$12$eG6cGhiT0RJKu2OPo7y7F.3jGBRdSZl9m1tWBz5cFXU7a4C3Zz7Ve',
                        'ADMIN',
                        'ACTIVE'
                    );
                    """);

                // ── Seed: sample blood bank ───────────────────────────────────
                stmt.execute("""
                    INSERT OR IGNORE INTO blood_banks (blood_bank_id, name, address, city, phone, status)
                    VALUES (1, 'City Central Blood Bank', '123 Medical Ave', 'Dhaka', '01700000001', 'ACTIVE');
                    """);

                logger.info("All database tables initialized successfully (SQLite).");
            }
        } catch (Exception e) {
            logger.error("Failed to initialize database tables: {}", e.getMessage(), e);
            throw new RuntimeException("Database initialization failed: " + e.getMessage(), e);
        }
    }
}
