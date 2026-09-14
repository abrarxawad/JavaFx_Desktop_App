package com.lifelink.model;

import java.time.LocalDateTime;

/**
 * Abstract base class for every person or entity that has an account in LifeLink.
 *
 * <p><b>OOP — Abstraction + Inheritance:</b> Common fields and behaviour shared by
 * Donor, Recipient, Admin, BloodBankStaff, and HospitalStaff are defined here once.
 * Subclasses add their role-specific attributes.
 *
 * <p><b>Package:</b> com.lifelink.model
 * <p><b>Subclasses:</b> Donor, Recipient, Admin (future phases)
 */
public abstract class User {

    // ── Fields (Encapsulation — all private) ──────────────────────────────────
    private int           userId;
    private String        username;
    private String        email;
    private String        passwordHash;   // NEVER store plain text
    private UserRole      role;
    private String        status;         // ACTIVE | INACTIVE | SUSPENDED
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // ── Constructors ──────────────────────────────────────────────────────────

    /** No-arg constructor required for DAO instantiation via reflection. */
    protected User() {}

    protected User(int userId, String username, String email,
                   String passwordHash, UserRole role, String status,
                   LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.userId       = userId;
        this.username     = username;
        this.email        = email;
        this.passwordHash = passwordHash;
        this.role         = role;
        this.status       = status;
        this.createdAt    = createdAt;
        this.updatedAt    = updatedAt;
    }

    // ── Abstract method ───────────────────────────────────────────────────────

    /**
     * Returns a short, human-readable display name for this user.
     * Subclasses override this to return first + last name, bank name, etc.
     */
    public abstract String getDisplayName();

    // ── Getters & Setters ─────────────────────────────────────────────────────

    public int getUserId()               { return userId; }
    public void setUserId(int userId)    { this.userId = userId; }

    public String getUsername()                  { return username; }
    public void setUsername(String username)      { this.username = username; }

    public String getEmail()                     { return email; }
    public void setEmail(String email)           { this.email = email; }

    public String getPasswordHash()              { return passwordHash; }
    public void setPasswordHash(String hash)     { this.passwordHash = hash; }

    public UserRole getRole()                    { return role; }
    public void setRole(UserRole role)           { this.role = role; }

    public String getStatus()                    { return status; }
    public void setStatus(String status)         { this.status = status; }

    public LocalDateTime getCreatedAt()          { return createdAt; }
    public void setCreatedAt(LocalDateTime dt)   { this.createdAt = dt; }

    public LocalDateTime getUpdatedAt()          { return updatedAt; }
    public void setUpdatedAt(LocalDateTime dt)   { this.updatedAt = dt; }

    // ── Utility ───────────────────────────────────────────────────────────────

    public boolean isActive() {
        return "ACTIVE".equalsIgnoreCase(status);
    }

    @Override
    public String toString() {
        return "User{id=" + userId + ", username='" + username
                + "', role=" + role + ", status='" + status + "'}";
    }
}
