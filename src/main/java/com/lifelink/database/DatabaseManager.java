package com.lifelink.database;

import com.lifelink.exception.DatabaseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

/**
 * Manages database connections for the entire application.
 *
 * <p><b>Design Pattern — Singleton:</b> Only one instance of this class can exist,
 * ensuring a single, centralised connection pool. This prevents multiple parts of
 * the application from each opening their own unlimited connections to SQLite.
 *
 * <p><b>Design Pattern — Connection Pool (simplified):</b> Instead of creating and
 * destroying a {@link Connection} for every query, we maintain a small pool of
 * reusable connections using a {@link BlockingQueue}. This dramatically reduces
 * latency for rapid consecutive queries while keeping the SQLite database responsive.
 *
 * <p><b>Thread Safety:</b> {@link BlockingQueue} is thread-safe by design, so
 * multiple concurrent background threads (e.g., the matching engine and inventory
 * monitor) can safely borrow and return connections without race conditions.
 *
 * <p><b>Package:</b> com.lifelink.database
 * <p><b>Used by:</b> Every DAO class (UserDAO, DonorDAO, etc.)
 *
 * <p><b>Configuration:</b> Reads from {@code /config/database.properties} on the
 * classpath so credentials are never hard-coded in source code.
 */
public class DatabaseManager {

    private static final Logger logger = LoggerFactory.getLogger(DatabaseManager.class);

    // ── Singleton instance (volatile for correct double-checked locking) ──────
    private static volatile DatabaseManager instance;

    // ── Connection pool ───────────────────────────────────────────────────────
    private final BlockingQueue<Connection> connectionPool;
    private final String    jdbcUrl;
    private final String    dbUsername;
    private final String    dbPassword;
    private final int       poolSize;

    // ── Private constructor (Singleton pattern) ───────────────────────────────
    private DatabaseManager() {
        Properties props = loadProperties();
        this.jdbcUrl    = props.getProperty("db.url");
        this.dbUsername = props.getProperty("db.username");
        this.dbPassword = props.getProperty("db.password", "");
        this.poolSize   = Integer.parseInt(props.getProperty("db.pool.size", "5"));

        connectionPool  = new ArrayBlockingQueue<>(poolSize);
        initPool();
        logger.info("DatabaseManager initialised. Pool size: {}", poolSize);
    }

    // ── Public accessor (double-checked locking for thread safety) ────────────

    /**
     * Returns the single instance of {@code DatabaseManager}.
     * Creates it on first call (lazy initialisation).
     */
    public static DatabaseManager getInstance() {
        if (instance == null) {
            synchronized (DatabaseManager.class) {
                if (instance == null) {
                    instance = new DatabaseManager();
                }
            }
        }
        return instance;
    }

    // ── Pool initialisation ───────────────────────────────────────────────────

    private void initPool() {
        for (int i = 0; i < poolSize; i++) {
            try {
                connectionPool.offer(createConnection());
            } catch (DatabaseException e) {
                logger.error("Failed to create initial connection #{}: {}", i, e.getMessage());
            }
        }
        if (connectionPool.isEmpty()) {
            throw new DatabaseException(
                "Could not establish any database connections. "
                + "Check the SQLite database path and file permissions in config/database.properties.",
                "Cannot connect to the SQLite database. Please confirm the database file exists and is accessible."
            );
        }
    }

    private Connection createConnection() {
        try {
            Connection conn = DriverManager.getConnection(jdbcUrl, dbUsername, dbPassword);
            conn.setAutoCommit(true);
            return conn;
        } catch (SQLException e) {
            throw new DatabaseException("Failed to create DB connection: " + e.getMessage(), e);
        }
    }

    // ── Connection borrow / return ────────────────────────────────────────────

    /**
     * Borrows a connection from the pool.
     *
     * <p>If the pool is empty (all connections in use), this blocks for up to
     * 30 seconds before throwing an exception. This prevents unbounded waits.
     *
     * @return a live {@link Connection}
     * @throws DatabaseException if no connection is available
     */
    public Connection getConnection() {
        try {
            Connection conn = connectionPool.poll(
                    30, java.util.concurrent.TimeUnit.SECONDS
            );
            if (conn == null) {
                throw new DatabaseException(
                    "Connection pool exhausted — no connection available within 30s.",
                    "The system is busy. Please try again in a moment."
                );
            }
            // Validate the connection; recreate if stale
            if (!conn.isValid(5)) {
                logger.warn("Stale connection detected — recreating.");
                conn = createConnection();
            }
            return conn;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new DatabaseException("Interrupted while waiting for DB connection.", e);
        } catch (SQLException e) {
            throw new DatabaseException("Error validating DB connection: " + e.getMessage(), e);
        }
    }

    /**
     * Returns a connection to the pool after use.
     *
     * <p>Always call this inside a {@code finally} block or use
     * try-with-resources via {@link #getConnectionWrapper()} to ensure
     * connections are never leaked.
     *
     * @param connection the connection to return
     */
    public void returnConnection(Connection connection) {
        if (connection != null) {
            connectionPool.offer(connection);
        }
    }

    /**
     * Returns an auto-closeable wrapper so DAOs can use try-with-resources:
     *
     * <pre>
     *   try (var ctx = DatabaseManager.getInstance().getConnectionWrapper()) {
     *       Connection conn = ctx.getConnection();
     *       // ... use conn ...
     *   }
     * </pre>
     */
    public ConnectionWrapper getConnectionWrapper() {
        return new ConnectionWrapper(getConnection(), this);
    }

    // ── Shutdown ──────────────────────────────────────────────────────────────

    /**
     * Closes all pooled connections. Call this when the application exits.
     */
    public void shutdown() {
        logger.info("Shutting down DatabaseManager — closing {} connection(s).",
                    connectionPool.size());
        connectionPool.forEach(conn -> {
            try { conn.close(); } catch (SQLException e) {
                logger.warn("Error closing pooled connection: {}", e.getMessage());
            }
        });
        connectionPool.clear();
    }

    // ── Properties loader ─────────────────────────────────────────────────────

    private Properties loadProperties() {
        Properties props = new Properties();
        try (InputStream in = getClass().getResourceAsStream("/config/database.properties")) {
            if (in == null) {
                throw new DatabaseException(
                    "database.properties not found on classpath.",
                    "Application configuration error. Contact support."
                );
            }
            props.load(in);
        } catch (IOException e) {
            throw new DatabaseException("Failed to load database.properties: " + e.getMessage(), e);
        }
        return props;
    }

    // ── Inner class: auto-closeable connection wrapper ────────────────────────

    /**
     * Wraps a {@link Connection} so it is automatically returned to the pool
     * when the try-with-resources block exits.
     */
    public static class ConnectionWrapper implements AutoCloseable {
        private final Connection       connection;
        private final DatabaseManager  manager;

        ConnectionWrapper(Connection connection, DatabaseManager manager) {
            this.connection = connection;
            this.manager    = manager;
        }

        public Connection getConnection() { return connection; }

        @Override
        public void close() {
            manager.returnConnection(connection);
        }
    }
}
