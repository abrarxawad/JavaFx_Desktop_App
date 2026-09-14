package com.lifelink.model;

/**
 * Represents the five roles a user can hold in LifeLink.
 *
 * <p>Role is stored in the {@code users.role} column and controls which
 * dashboard/navigation panel is shown after login.
 *
 * <p>Package: com.lifelink.model
 */
public enum UserRole {
    DONOR,
    RECIPIENT,
    BLOOD_BANK,
    HOSPITAL,
    ADMIN
}
