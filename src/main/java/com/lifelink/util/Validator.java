package com.lifelink.util;

import com.lifelink.exception.ValidationException;

import java.time.LocalDate;
import java.time.Period;
import java.util.regex.Pattern;

/**
 * Centralised input validation utility for the LifeLink application.
 *
 * <p>All validation is done here — in the service layer — before data
 * reaches the DAO/database layer. This ensures that invalid data can
 * never corrupt the database.
 *
 * <p>Methods throw {@link ValidationException} on failure so controllers
 * can catch specific validation failures and display friendly messages.
 *
 * <p><b>Package:</b> com.lifelink.util
 * <p><b>Used by:</b> AuthService, DonorService, BloodRequestService, etc.
 */
public class Validator {

    // ── Compiled patterns (compiled once, reused) ──────────────────────────────
    private static final Pattern EMAIL_PATTERN   =
        Pattern.compile("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");

    private static final Pattern PHONE_PATTERN   =
        Pattern.compile("^\\+?[0-9]{7,15}$");

    private static final Pattern PASSWORD_PATTERN =
        Pattern.compile("^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@#$!%*?&]).{8,}$");

    private static final Pattern USERNAME_PATTERN =
        Pattern.compile("^[a-zA-Z0-9_]{3,30}$");

    /** Prevent instantiation. */
    private Validator() {}

    // ── Required field ─────────────────────────────────────────────────────────

    /**
     * Validates that a string field is not null, empty, or blank.
     *
     * @param fieldName name of the field (used in error message)
     * @param value     the value to check
     */
    public static void requireNonBlank(String fieldName, String value) {
        if (value == null || value.isBlank()) {
            throw new ValidationException(fieldName, fieldName + " is required.");
        }
    }

    // ── Email ─────────────────────────────────────────────────────────────────

    public static void validateEmail(String email) {
        requireNonBlank("Email", email);
        if (!EMAIL_PATTERN.matcher(email.trim()).matches()) {
            throw new ValidationException("Email", "Please enter a valid email address.");
        }
    }

    // ── Phone ─────────────────────────────────────────────────────────────────

    public static void validatePhone(String phone) {
        requireNonBlank("Phone", phone);
        if (!PHONE_PATTERN.matcher(phone.trim()).matches()) {
            throw new ValidationException("Phone",
                "Phone number must be 7-15 digits (optional leading +).");
        }
    }

    // ── Password ──────────────────────────────────────────────────────────────

    /**
     * Validates password strength:
     * at least 8 characters, 1 uppercase, 1 lowercase, 1 digit, 1 special character.
     */
    public static void validatePassword(String password) {
        if (password == null || password.length() < 8) {
            throw new ValidationException("Password",
                "Password must be at least 8 characters long.");
        }
        if (!PASSWORD_PATTERN.matcher(password).matches()) {
            throw new ValidationException("Password",
                "Password must contain uppercase, lowercase, digit, and special character.");
        }
    }

    // ── Username ──────────────────────────────────────────────────────────────

    public static void validateUsername(String username) {
        requireNonBlank("Username", username);
        if (!USERNAME_PATTERN.matcher(username.trim()).matches()) {
            throw new ValidationException("Username",
                "Username must be 3-30 characters (letters, digits, underscores only).");
        }
    }

    // ── Age ───────────────────────────────────────────────────────────────────

    /**
     * Validates that a donor's age is between 18 and 65.
     *
     * @param dateOfBirth the donor's date of birth
     */
    public static void validateDonorAge(LocalDate dateOfBirth) {
        if (dateOfBirth == null) {
            throw new ValidationException("Date of Birth", "Date of birth is required.");
        }
        int age = Period.between(dateOfBirth, LocalDate.now()).getYears();
        if (age < 18) {
            throw new ValidationException("Date of Birth",
                "Donor must be at least 18 years old.");
        }
        if (age > 65) {
            throw new ValidationException("Date of Birth",
                "Donor must be 65 years old or younger.");
        }
    }

    // ── Quantity ──────────────────────────────────────────────────────────────

    public static void validateQuantity(int quantity, int min, int max) {
        if (quantity < min || quantity > max) {
            throw new ValidationException("Quantity",
                "Quantity must be between " + min + " and " + max + ".");
        }
    }

    // ── Date range ────────────────────────────────────────────────────────────

    public static void validateFutureDate(String fieldName, LocalDate date) {
        if (date == null) {
            throw new ValidationException(fieldName, fieldName + " is required.");
        }
        if (!date.isAfter(LocalDate.now())) {
            throw new ValidationException(fieldName, fieldName + " must be a future date.");
        }
    }
}
