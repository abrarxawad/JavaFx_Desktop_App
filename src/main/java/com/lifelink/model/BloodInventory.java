package com.lifelink.model;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Represents a batch of one blood group held in a blood bank's inventory.
 *
 * <p>Each row tracks collection/expiry dates so that expired or soon-to-expire
 * blood can be detected by the background inventory-monitoring task.
 *
 * <p><b>Package:</b> com.lifelink.model
 * <p><b>Used by:</b> BloodInventoryDAO, BloodInventoryService, InventoryMonitorTask
 */
public class BloodInventory {

    // ── Status constants ──────────────────────────────────────────────────────
    public static final String STATUS_AVAILABLE = "AVAILABLE";
    public static final String STATUS_RESERVED  = "RESERVED";
    public static final String STATUS_EXPIRED   = "EXPIRED";
    public static final String STATUS_ISSUED    = "ISSUED";

    // ── Fields ────────────────────────────────────────────────────────────────
    private int           inventoryId;
    private int           bloodBankId;
    private String        bloodBankName;   // denormalised for display
    private BloodGroup    bloodGroup;
    private int           quantity;
    private LocalDate     collectionDate;
    private LocalDate     expiryDate;
    private String        status;          // AVAILABLE | RESERVED | EXPIRED | ISSUED
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public BloodInventory() {}

    // ── Derived business logic ────────────────────────────────────────────────

    /**
     * Returns true if today's date is past the expiry date.
     */
    public boolean isExpired() {
        return expiryDate != null && LocalDate.now().isAfter(expiryDate);
    }

    /**
     * Returns true if the batch expires within the given number of days.
     *
     * @param warningDays number of days warning threshold (e.g. 7)
     */
    public boolean isExpiringSoon(int warningDays) {
        if (expiryDate == null) return false;
        LocalDate warningDate = LocalDate.now().plusDays(warningDays);
        return !isExpired() && expiryDate.isBefore(warningDate);
    }

    // ── Getters & Setters ─────────────────────────────────────────────────────

    public int getInventoryId()                     { return inventoryId; }
    public void setInventoryId(int id)              { this.inventoryId = id; }

    public int getBloodBankId()                     { return bloodBankId; }
    public void setBloodBankId(int id)              { this.bloodBankId = id; }

    public String getBloodBankName()                { return bloodBankName; }
    public void setBloodBankName(String n)          { this.bloodBankName = n; }

    public BloodGroup getBloodGroup()               { return bloodGroup; }
    public void setBloodGroup(BloodGroup bg)        { this.bloodGroup = bg; }

    public int getQuantity()                        { return quantity; }
    public void setQuantity(int qty)                { this.quantity = qty; }

    public LocalDate getCollectionDate()            { return collectionDate; }
    public void setCollectionDate(LocalDate d)      { this.collectionDate = d; }

    public LocalDate getExpiryDate()                { return expiryDate; }
    public void setExpiryDate(LocalDate d)          { this.expiryDate = d; }

    public String getStatus()                       { return status; }
    public void setStatus(String status)            { this.status = status; }

    public LocalDateTime getCreatedAt()             { return createdAt; }
    public void setCreatedAt(LocalDateTime dt)      { this.createdAt = dt; }

    public LocalDateTime getUpdatedAt()             { return updatedAt; }
    public void setUpdatedAt(LocalDateTime dt)      { this.updatedAt = dt; }

    @Override
    public String toString() {
        return "BloodInventory{id=" + inventoryId + ", bank=" + bloodBankId
                + ", group=" + bloodGroup + ", qty=" + quantity
                + ", expiry=" + expiryDate + ", status='" + status + "'}";
    }
}
