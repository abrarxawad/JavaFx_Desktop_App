package com.lifelink.exception;

/**
 * Thrown when a database operation fails (connection error, SQL error, etc.).
 *
 * <p>Wraps {@link java.sql.SQLException} so callers never need to import
 * java.sql in non-DAO layers.
 *
 * <p>Package: com.lifelink.exception
 */
public class DatabaseException extends LifeLinkException {

    public DatabaseException(String message) {
        super(message, "A database error occurred. Please try again.");
    }

    public DatabaseException(String message, Throwable cause) {
        super(message, "A database error occurred. Please try again.", cause);
    }

    public DatabaseException(String message, String userMessage) {
        super(message, userMessage);
    }

    public DatabaseException(String message, String userMessage, Throwable cause) {
        super(message, userMessage, cause);
    }
}
