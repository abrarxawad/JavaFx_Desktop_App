package com.lifelink.exception;

/**
 * Thrown when login credentials are invalid or an account is suspended/inactive.
 *
 * <p>Package: com.lifelink.exception
 */
public class AuthenticationException extends LifeLinkException {

    public AuthenticationException(String message) {
        super(message, message);   // safe to show as-is
    }

    public AuthenticationException(String message, Throwable cause) {
        super(message, message, cause);
    }
}
