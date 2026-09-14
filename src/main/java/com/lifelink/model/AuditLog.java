package com.lifelink.model;

import java.time.LocalDateTime;

/**
 * Immutable audit log entry recording every important action in the system.
 *
 * <p>Every user-initiated or system-initiated action that modifies data
 * (login, inventory update, request status change, user deactivation, etc.)
 * should create an AuditLog entry. This ensures full traceability.
 *
 * <p><b>Package:</b> com.lifelink.model
 * <p><b>Used by:</b> AuditLogDAO, AuditLogService, AdminAuditController
 */
public class AuditLog {

    private int           logId;
    private Integer       userId;       // nullable for system-generated entries
    private String        action;       // e.g. "USER_LOGIN", "INVENTORY_UPDATE"
    private String        entityType;   // e.g. "BloodRequest", "User"
    private Integer       entityId;     // the ID of the affected entity
    private String        description;  // human-readable description
    private String        ipAddress;    // nullable
    private LocalDateTime timestamp;

    public AuditLog() {}

    public AuditLog(Integer userId, String action, String entityType,
                    Integer entityId, String description) {
        this.userId     = userId;
        this.action     = action;
        this.entityType = entityType;
        this.entityId   = entityId;
        this.description = description;
        this.timestamp  = LocalDateTime.now();
    }

    // ── Getters & Setters ─────────────────────────────────────────────────────

    public int getLogId()                          { return logId; }
    public void setLogId(int id)                   { this.logId = id; }

    public Integer getUserId()                     { return userId; }
    public void setUserId(Integer uid)             { this.userId = uid; }

    public String getAction()                      { return action; }
    public void setAction(String action)           { this.action = action; }

    public String getEntityType()                  { return entityType; }
    public void setEntityType(String et)           { this.entityType = et; }

    public Integer getEntityId()                   { return entityId; }
    public void setEntityId(Integer eid)           { this.entityId = eid; }

    public String getDescription()                 { return description; }
    public void setDescription(String desc)        { this.description = desc; }

    public String getIpAddress()                   { return ipAddress; }
    public void setIpAddress(String ip)            { this.ipAddress = ip; }

    public LocalDateTime getTimestamp()            { return timestamp; }
    public void setTimestamp(LocalDateTime ts)     { this.timestamp = ts; }

    @Override
    public String toString() {
        return "AuditLog{id=" + logId + ", action='" + action
                + "', entity=" + entityType + "#" + entityId
                + ", ts=" + timestamp + "}";
    }
}
