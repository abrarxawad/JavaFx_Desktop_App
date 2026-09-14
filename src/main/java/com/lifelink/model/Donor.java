package com.lifelink.model;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Period;

/**
 * Domain model for a blood donor — extends {@link User}.
 *
 * <p><b>OOP — Inheritance:</b> Inherits authentication fields from User and
 * adds all donor-specific attributes such as blood group, location, last
 * donation date, and eligibility status.
 *
 * <p>The eligibility calculation (donor must wait ≥56 days between donations)
 * is a business rule encapsulated here so it is not duplicated across the UI
 * and service layer.
 *
 * <p><b>Package:</b> com.lifelink.model
 * <p><b>Used by:</b> DonorDAO, DonorService, DonorController, MatchingEngine
 */
public class Donor extends User {

    // ── Donor-specific fields ─────────────────────────────────────────────────
    private int        donorId;
    private String     firstName;
    private String     lastName;
    private BloodGroup bloodGroup;
    private LocalDate  dateOfBirth;
    private String     gender;        // MALE | FEMALE | OTHER
    private String     phone;
    private String     address;
    private String     city;
    private Double     latitude;
    private Double     longitude;
    private LocalDate  lastDonationDate;
    private boolean    available;
    private String     eligibilityStatus; // ELIGIBLE | INELIGIBLE | PENDING_CHECK
    private int        totalDonations;

    // ── Constructors ──────────────────────────────────────────────────────────

    public Donor() {
        super();
    }

    // ── Override from User ────────────────────────────────────────────────────

    /**
     * Returns "FirstName LastName (BloodGroup)" for display in tables/headers.
     */
    @Override
    public String getDisplayName() {
        return firstName + " " + lastName +
               (bloodGroup != null ? " (" + bloodGroup.getLabel() + ")" : "");
    }

    // ── Business logic ────────────────────────────────────────────────────────

    /**
     * Computes current age from date of birth.
     *
     * @return age in full years, or -1 if DOB is not set
     */
    public int getAge() {
        if (dateOfBirth == null) return -1;
        return Period.between(dateOfBirth, LocalDate.now()).getYears();
    }

    /**
     * Returns true if at least 56 days have passed since the last donation
     * AND the donor is marked as ELIGIBLE and available.
     *
     * <p>This is the rule for whole-blood donation (8-week minimum interval).
     * The threshold is configurable in app_config but defaults to 56 days.
     *
     * @param minDaysBetweenDonations normally 56
     */
    public boolean isEligibleToDonatee(int minDaysBetweenDonations) {
        if (!"ELIGIBLE".equalsIgnoreCase(eligibilityStatus)) return false;
        if (!available) return false;
        if (lastDonationDate == null) return true; // never donated — eligible
        long daysSince = LocalDate.now().toEpochDay() - lastDonationDate.toEpochDay();
        return daysSince >= minDaysBetweenDonations;
    }

    // ── Getters & Setters ─────────────────────────────────────────────────────

    public int getDonorId()                           { return donorId; }
    public void setDonorId(int id)                    { this.donorId = id; }

    public String getFirstName()                      { return firstName; }
    public void setFirstName(String fn)               { this.firstName = fn; }

    public String getLastName()                       { return lastName; }
    public void setLastName(String ln)                { this.lastName = ln; }

    public BloodGroup getBloodGroup()                 { return bloodGroup; }
    public void setBloodGroup(BloodGroup bg)          { this.bloodGroup = bg; }

    public LocalDate getDateOfBirth()                 { return dateOfBirth; }
    public void setDateOfBirth(LocalDate dob)         { this.dateOfBirth = dob; }

    public String getGender()                         { return gender; }
    public void setGender(String gender)              { this.gender = gender; }

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

    public LocalDate getLastDonationDate()            { return lastDonationDate; }
    public void setLastDonationDate(LocalDate date)   { this.lastDonationDate = date; }

    public boolean isAvailable()                      { return available; }
    public void setAvailable(boolean available)       { this.available = available; }

    public String getEligibilityStatus()              { return eligibilityStatus; }
    public void setEligibilityStatus(String status)   { this.eligibilityStatus = status; }

    public int getTotalDonations()                    { return totalDonations; }
    public void setTotalDonations(int n)              { this.totalDonations = n; }

    // ── Convenience: checks if location coordinates are stored ───────────────
    public boolean hasLocation() {
        return latitude != null && longitude != null;
    }

    @Override
    public String toString() {
        return "Donor{id=" + donorId + ", name='" + getDisplayName() +
               "', bloodGroup=" + bloodGroup + ", available=" + available + "}";
    }
}
