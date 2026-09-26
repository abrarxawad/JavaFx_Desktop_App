package com.lifelink.json;

public class JsonAppConfig {
    private double emergencyMatchingRadiusKm = 25.0;
    private int lowStockThreshold = 8;
    private boolean notificationsEnabled = true;
    private boolean emailAlertsEnabled = true;
    private boolean smsAlertsEnabled = true;

    public JsonAppConfig() {
    }

    public JsonAppConfig(double emergencyMatchingRadiusKm, int lowStockThreshold,
                         boolean notificationsEnabled, boolean emailAlertsEnabled,
                         boolean smsAlertsEnabled) {
        this.emergencyMatchingRadiusKm = emergencyMatchingRadiusKm;
        this.lowStockThreshold = lowStockThreshold;
        this.notificationsEnabled = notificationsEnabled;
        this.emailAlertsEnabled = emailAlertsEnabled;
        this.smsAlertsEnabled = smsAlertsEnabled;
    }

    public double getEmergencyMatchingRadiusKm() {
        return emergencyMatchingRadiusKm;
    }

    public void setEmergencyMatchingRadiusKm(double emergencyMatchingRadiusKm) {
        this.emergencyMatchingRadiusKm = emergencyMatchingRadiusKm;
    }

    public int getLowStockThreshold() {
        return lowStockThreshold;
    }

    public void setLowStockThreshold(int lowStockThreshold) {
        this.lowStockThreshold = lowStockThreshold;
    }

    public boolean isNotificationsEnabled() {
        return notificationsEnabled;
    }

    public void setNotificationsEnabled(boolean notificationsEnabled) {
        this.notificationsEnabled = notificationsEnabled;
    }

    public boolean isEmailAlertsEnabled() {
        return emailAlertsEnabled;
    }

    public void setEmailAlertsEnabled(boolean emailAlertsEnabled) {
        this.emailAlertsEnabled = emailAlertsEnabled;
    }

    public boolean isSmsAlertsEnabled() {
        return smsAlertsEnabled;
    }

    public void setSmsAlertsEnabled(boolean smsAlertsEnabled) {
        this.smsAlertsEnabled = smsAlertsEnabled;
    }
}
