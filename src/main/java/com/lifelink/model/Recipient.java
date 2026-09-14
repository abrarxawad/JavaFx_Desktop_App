package com.lifelink.model;

import java.time.LocalDateTime;

/**
 * Domain model for a blood recipient / patient — extends {@link User}.
 *
 * <p><b>OOP — Inheritance:</b> Inherits core account fields from User and adds
 * recipient-specific attributes such as blood group and location.
 *
 * <p><b>Package:</b> com.lifelink.model
 * <p><b>Used by:</b> RecipientDAO, RecipientService, BloodRequest
 */
public class Recipient extends User {

    private int        recipientId;
    private String     firstName;
    private String     lastName;
    private BloodGroup bloodGroup;
    private String     phone;
    private String     address;
    private String     city;
    private Double     latitude;
    private Double     longitude;

    public Recipient() {
        super();
    }

    @Override
    public String getDisplayName() {
        return firstName + " " + lastName;
    }

    // ── Getters & Setters ─────────────────────────────────────────────────────

    public int getRecipientId()                       { return recipientId; }
    public void setRecipientId(int id)                { this.recipientId = id; }

    public String getFirstName()                      { return firstName; }
    public void setFirstName(String fn)               { this.firstName = fn; }

    public String getLastName()                       { return lastName; }
    public void setLastName(String ln)                { this.lastName = ln; }

    public BloodGroup getBloodGroup()                 { return bloodGroup; }
    public void setBloodGroup(BloodGroup bg)          { this.bloodGroup = bg; }

    public String getPhone()                          { return phone; }
    public void setPhone(String phone)                { this.phone = phone; }

    public String getAddress()                        { return address; }
    public void setAddress(String address)            { this.address = address; }

    public String getCity()                           { return city; }
    public void setCity(String city)                  { this.city = city; }

    public Double getLatitude()                       { return latitude; }
    public void setLatitude(Double lat)               { this.latitude = lat; }

    public Double getLongitude()                      { return longitude; }
    public void setLongitude(Double lon)              { this.longitude = lon; }

    public boolean hasLocation() {
        return latitude != null && longitude != null;
    }

    @Override
    public String toString() {
        return "Recipient{id=" + recipientId + ", name='" + getDisplayName()
                + "', bloodGroup=" + bloodGroup + "}";
    }
}
