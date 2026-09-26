package com.lifelink.model;

import java.time.LocalDateTime;

/**
 * Represents a blood request created by a Recipient or Hospital.
 *
 * <p><b>OOP — Builder pattern:</b> The inner {@link Builder} class is provided
 * so complex BloodRequest objects can be constructed without a huge constructor.
 *
 * <p>Priority level (NORMAL / URGENT / EMERGENCY) determines the order in
 * which the matching engine processes requests via a PriorityBlockingQueue.
 *
 * <p><b>Package:</b> com.lifelink.model
 * <p><b>Used by:</b> BloodRequestDAO, BloodRequestService, MatchingEngine,
 *                    RecipientController, AdminDashboard
 */
public class BloodRequest {

    // ── Status constants ──────────────────────────────────────────────────────
    public static final String PRIORITY_NORMAL    = "NORMAL";
    public static final String PRIORITY_URGENT    = "URGENT";
    public static final String PRIORITY_EMERGENCY = "EMERGENCY";

    public static final String STATUS_PENDING              = "PENDING";
    public static final String STATUS_MATCHING             = "MATCHING";
    public static final String STATUS_PARTIALLY_FULFILLED  = "PARTIALLY_FULFILLED";
    public static final String STATUS_AWAITING_CONFIRMATION = "AWAITING_CONFIRMATION";
    public static final String STATUS_FULFILLED            = "FULFILLED";
    public static final String STATUS_CANCELLED            = "CANCELLED";
    public static final String STATUS_REJECTED             = "REJECTED";

    // ── Fields ────────────────────────────────────────────────────────────────
    private int            requestId;
    private int            requesterId;      // users.user_id
    private String         requesterName;    // denormalised for display
    private BloodGroup     bloodGroup;
    private int            quantity;
    private Integer        hospitalId;       // nullable
    private String         hospitalName;     // denormalised for display
    private String         requesterLocation;
    private String         requesterCity;
    private String         requesterType;
    private String         priority;
    private String         status;
    private LocalDateTime  requestDate;
    private LocalDateTime  requiredDate;
    private String         notes;
    private LocalDateTime  updatedAt;

    private BloodRequest() {}

    // ── Builder ───────────────────────────────────────────────────────────────

    /**
     * Builder pattern for constructing a BloodRequest object.
     *
     * <p>Usage example:
     * <pre>
     *   BloodRequest req = new BloodRequest.Builder()
     *       .requesterId(userId)
     *       .bloodGroup(BloodGroup.O_NEGATIVE)
     *       .quantity(2)
     *       .priority(BloodRequest.PRIORITY_EMERGENCY)
     *       .build();
     * </pre>
     */
    public static class Builder {
        private final BloodRequest request = new BloodRequest();

        public Builder requesterId(int id)          { request.requesterId = id;    return this; }
        public Builder requesterName(String n)      { request.requesterName = n;   return this; }
        public Builder bloodGroup(BloodGroup bg)    { request.bloodGroup = bg;     return this; }
        public Builder quantity(int qty)            { request.quantity = qty;      return this; }
        public Builder hospitalId(Integer hid)      { request.hospitalId = hid;    return this; }
        public Builder hospitalName(String hn)      { request.hospitalName = hn;   return this; }
        public Builder requesterLocation(String loc){ request.requesterLocation = loc; return this; }
        public Builder requesterCity(String city)   { request.requesterCity = city; return this; }
        public Builder requesterType(String type)   { request.requesterType = type; return this; }
        public Builder priority(String p)           { request.priority = p;        return this; }
        public Builder status(String s)             { request.status = s;          return this; }
        public Builder requestDate(LocalDateTime dt){ request.requestDate = dt;    return this; }
        public Builder requiredDate(LocalDateTime d){ request.requiredDate = d;    return this; }
        public Builder notes(String n)              { request.notes = n;           return this; }

        public BloodRequest build() {
            // Apply defaults
            if (request.status == null)      request.status = STATUS_PENDING;
            if (request.priority == null)    request.priority = PRIORITY_NORMAL;
            if (request.requestDate == null) request.requestDate = LocalDateTime.now();
            if (request.quantity <= 0)       request.quantity = 1;
            return request;
        }
    }

    // ── Derived ───────────────────────────────────────────────────────────────

    /** Returns true if this request is at EMERGENCY priority. */
    public boolean isEmergency() {
        return PRIORITY_EMERGENCY.equalsIgnoreCase(priority);
    }

    /** Returns a numeric priority value for queue ordering (higher = more urgent). */
    public int getPriorityValue() {
        return switch (priority == null ? "" : priority.toUpperCase()) {
            case "EMERGENCY" -> 3;
            case "URGENT"    -> 2;
            default          -> 1;
        };
    }

    // ── Getters & Setters ─────────────────────────────────────────────────────

    public int getRequestId()                      { return requestId; }
    public void setRequestId(int id)               { this.requestId = id; }

    public int getRequesterId()                    { return requesterId; }
    public void setRequesterId(int id)             { this.requesterId = id; }

    public String getRequesterName()               { return requesterName; }
    public void setRequesterName(String n)         { this.requesterName = n; }

    public BloodGroup getBloodGroup()              { return bloodGroup; }
    public void setBloodGroup(BloodGroup bg)       { this.bloodGroup = bg; }

    public int getQuantity()                       { return quantity; }
    public void setQuantity(int qty)               { this.quantity = qty; }

    public Integer getHospitalId()                 { return hospitalId; }
    public void setHospitalId(Integer hid)         { this.hospitalId = hid; }

    public String getHospitalName()                { return hospitalName; }
    public void setHospitalName(String hn)         { this.hospitalName = hn; }

    public String getRequesterLocation()           { return requesterLocation; }
    public void setRequesterLocation(String loc)   { this.requesterLocation = loc; }

    public String getRequesterCity()              { return requesterCity; }
    public void setRequesterCity(String city)     { this.requesterCity = city; }

    public String getRequesterType()               { return requesterType; }
    public void setRequesterType(String type)      { this.requesterType = type; }

    public String getPriority()                    { return priority; }
    public void setPriority(String p)              { this.priority = p; }

    public String getStatus()                      { return status; }
    public void setStatus(String s)                { this.status = s; }

    public LocalDateTime getRequestDate()          { return requestDate; }
    public void setRequestDate(LocalDateTime dt)   { this.requestDate = dt; }

    public LocalDateTime getRequiredDate()         { return requiredDate; }
    public void setRequiredDate(LocalDateTime d)   { this.requiredDate = d; }

    public String getNotes()                       { return notes; }
    public void setNotes(String n)                 { this.notes = n; }

    public LocalDateTime getUpdatedAt()            { return updatedAt; }
    public void setUpdatedAt(LocalDateTime dt)     { this.updatedAt = dt; }

    @Override
    public String toString() {
        return "BloodRequest{id=" + requestId + ", group=" + bloodGroup
                + ", qty=" + quantity + ", priority='" + priority
                + "', status='" + status + "'}";
    }
}
