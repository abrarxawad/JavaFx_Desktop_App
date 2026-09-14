package com.lifelink.exception;

/**
 * Thrown when user-supplied input fails validation rules
 * (e.g., invalid blood group, missing required field, bad date).
 *
 * <p>Package: com.lifelink.exception
 */
public class ValidationException extends LifeLinkException {

    private final String fieldName; // which field caused the problem

    public ValidationException(String fieldName, String message) {
        super("Validation failed for field '" + fieldName + "': " + message, message);
        this.fieldName = fieldName;
    }

    public ValidationException(String message) {
        super(message, message);
        this.fieldName = null;
    }

    /** Returns the field name that failed validation, or null if not field-specific. */
    public String getFieldName() {
        return fieldName;
    }
}
