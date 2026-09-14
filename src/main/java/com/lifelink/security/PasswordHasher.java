package com.lifelink.security;

import at.favre.lib.crypto.bcrypt.BCrypt;

/**
 * Utility class for password hashing and verification using BCrypt.
 *
 * <p><b>Why BCrypt?</b> BCrypt is a slow, adaptive hashing algorithm specifically
 * designed for passwords. Its built-in work factor (cost parameter) can be
 * increased over time as hardware gets faster, ensuring long-term security.
 * Unlike MD5 or SHA-256, BCrypt is resistant to rainbow-table and brute-force attacks.
 *
 * <p>Plain-text passwords are NEVER stored. When a user registers, the password
 * is hashed here before being sent to the DAO. When a user logs in, the
 * submitted password is verified against the stored hash — the original password
 * cannot be recovered from the hash.
 *
 * <p><b>Package:</b> com.lifelink.security
 * <p><b>Used by:</b> AuthService (Phase 5)
 */
public class PasswordHasher {

    /**
     * BCrypt cost factor (12 is a good balance between security and speed).
     * Higher = slower to compute (harder to brute force, but slower to verify).
     */
    private static final int BCRYPT_COST = 12;

    /** Prevent instantiation — all methods are static utility methods. */
    private PasswordHasher() {}

    /**
     * Hashes a plain-text password.
     *
     * @param plainPassword the user's raw password input
     * @return the BCrypt hash string (includes salt and cost, safe to store)
     * @throws IllegalArgumentException if password is null or blank
     */
    public static String hash(String plainPassword) {
        if (plainPassword == null || plainPassword.isBlank()) {
            throw new IllegalArgumentException("Password must not be null or blank.");
        }
        return BCrypt.withDefaults().hashToString(BCRYPT_COST, plainPassword.toCharArray());
    }

    /**
     * Verifies a plain-text password against a stored BCrypt hash.
     *
     * @param plainPassword the password typed by the user at login
     * @param storedHash    the hash retrieved from the database
     * @return {@code true} if the password matches the hash, {@code false} otherwise
     */
    public static boolean verify(String plainPassword, String storedHash) {
        if (plainPassword == null || storedHash == null) return false;
        BCrypt.Result result = BCrypt.verifyer().verify(
                plainPassword.toCharArray(), storedHash
        );
        return result.verified;
    }
}
