package com.lifelink.model;

import java.time.LocalDateTime;

/**
 * Domain model for a Blood Bank entity.
 *
 * <p>A blood bank may or may not have a linked {@link User} account.
 * The {@code userId} field is nullable — some blood banks are created by
 * an Admin and don't have a login account of their own.
 *
 * <p><b>Package:</b> com.lifelink.model
 * <p><b>Used by:</b> BloodBankDAO, BloodBankService, BloodInventory
 */
public class BloodBank {

    private int           bloodBankId;
    private Integer       userId;         // nullable — linked account
    private String        name;
    private String        address;
    private String        city;
    private Double        latitude;
    private Double        longitude;
    private String        phone;
    private String        email;
    private String        status;         // ACTIVE | INACTIVE
    private LocalDateTime createdAt;

    public BloodBank() {}

    // ── Getters & Setters ─────────────────────────────────────────────────────

    public int getBloodBankId()                     { return bloodBankId; }
    public void setBloodBankId(int id)              { this.bloodBankId = id; }

    public Integer getUserId()                      { return userId; }
    public void setUserId(Integer userId)           { this.userId = userId; }

    public String getName()                         { return name; }
    public void setName(String name)                { this.name = name; }

    public String getAddress()                      { return address; }
    public void setAddress(String address)          { this.address = address; }

    public String getCity()                         { return city; }
    public void setCity(String city)                { this.city = city; }

    public Double getLatitude()                     { return latitude; }
    public void setLatitude(Double lat)             { this.latitude = lat; }

    public Double getLongitude()                    { return longitude; }
    public void setLongitude(Double lon)            { this.longitude = lon; }

    public String getPhone()                        { return phone; }
    public void setPhone(String phone)              { this.phone = phone; }

    public String getEmail()                        { return email; }
    public void setEmail(String email)              { this.email = email; }

    public String getStatus()                       { return status; }
    public void setStatus(String status)            { this.status = status; }

    public LocalDateTime getCreatedAt()             { return createdAt; }
    public void setCreatedAt(LocalDateTime dt)      { this.createdAt = dt; }

    public boolean hasLocation() {
        return latitude != null && longitude != null;
    }

    public boolean isActive() {
        return "ACTIVE".equalsIgnoreCase(status);
    }

    @Override
    public String toString() {
        return "BloodBank{id=" + bloodBankId + ", name='" + name
                + "', city='" + city + "', status='" + status + "'}";
    }
}
