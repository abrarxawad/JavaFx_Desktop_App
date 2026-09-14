package com.lifelink.exception;

/**
 * Root unchecked exception for all application-level errors.
 *
 * <p>All custom exceptions in LifeLink extend this class so callers can
 * catch either a specific type or the entire application exception family
 * with a single catch block.
 *
 * <p>Package: com.lifelink.exception
 */
public class LifeLinkException extends RuntimeException {

    private final String userMessage; // safe, friendly message shown to end-users

    public LifeLinkException(String message) {
        super(message);
        this.userMessage = message;
    }

    public LifeLinkException(String message, Throwable cause) {
        super(message, cause);
        this.userMessage = message;
    }

    public LifeLinkException(String developerMessage, String userMessage) {
        super(developerMessage);
        this.userMessage = userMessage;
    }

    public LifeLinkException(String developerMessage, String userMessage, Throwable cause) {
        super(developerMessage, cause);
        this.userMessage = userMessage;
    }

    /** Returns a safe, friendly message suitable for display in the UI. */
    public String getUserMessage() {
        return userMessage;
    }
}
