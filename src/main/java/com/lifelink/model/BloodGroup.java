package com.lifelink.model;

/**
 * All supported human blood groups.
 *
 * <p>The {@link #getLabel()} method returns the standard medical notation
 * (e.g., "A+" or "O-") which is what the UI and database store.
 *
 * <p>Package: com.lifelink.model
 * <p>Used by: Donor, Recipient, BloodRequest, BloodInventory
 */
public enum BloodGroup {

    A_POSITIVE("A+"),
    A_NEGATIVE("A-"),
    B_POSITIVE("B+"),
    B_NEGATIVE("B-"),
    AB_POSITIVE("AB+"),
    AB_NEGATIVE("AB-"),
    O_POSITIVE("O+"),
    O_NEGATIVE("O-");

    private final String label;

    BloodGroup(String label) {
        this.label = label;
    }

    /** Returns the medical notation string, e.g. {@code "AB+"}. */
    public String getLabel() {
        return label;
    }

    /**
     * Looks up a {@code BloodGroup} by its medical label string.
     *
     * @param label e.g. "A+" or "O-"
     * @return matching {@code BloodGroup}
     * @throws IllegalArgumentException if label is not recognised
     */
    public static BloodGroup fromLabel(String label) {
        for (BloodGroup bg : values()) {
            if (bg.label.equalsIgnoreCase(label)) {
                return bg;
            }
        }
        throw new IllegalArgumentException("Unknown blood group label: " + label);
    }

    @Override
    public String toString() {
        return label;
    }
}
