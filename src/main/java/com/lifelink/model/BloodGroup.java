package com.lifelink.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

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
    @JsonValue
    public String getLabel() {
        return label;
    }

    /**
     * Checks whether this donor group can donate to the target recipient group.
     * This follows the real transfusion rules used in blood bank matching.
     */
    public boolean canDonateTo(BloodGroup recipientBloodGroup) {
        if (recipientBloodGroup == null) {
            return false;
        }
        return getCompatibleRecipientGroups().contains(recipientBloodGroup);
    }

    /**
     * Returns the recipient blood groups this donor group can safely serve.
     */
    public Set<BloodGroup> getCompatibleRecipientGroups() {
        return switch (this) {
            case O_NEGATIVE -> EnumSet.allOf(BloodGroup.class);
            case O_POSITIVE -> EnumSet.of(O_POSITIVE, A_POSITIVE, B_POSITIVE);
            case A_NEGATIVE -> EnumSet.of(A_NEGATIVE, A_POSITIVE, AB_NEGATIVE, AB_POSITIVE);
            case A_POSITIVE -> EnumSet.of(A_POSITIVE, AB_POSITIVE);
            case B_NEGATIVE -> EnumSet.of(B_NEGATIVE, B_POSITIVE, AB_NEGATIVE, AB_POSITIVE);
            case B_POSITIVE -> EnumSet.of(B_POSITIVE, AB_POSITIVE);
            case AB_NEGATIVE -> EnumSet.of(AB_NEGATIVE, AB_POSITIVE);
            case AB_POSITIVE -> EnumSet.of(AB_POSITIVE);
        };
    }

    /**
     * Returns all donor blood groups that can donate to the given recipient group.
     */
    public static Set<BloodGroup> getCompatibleDonorGroups(BloodGroup recipientBloodGroup) {
        if (recipientBloodGroup == null) {
            return EnumSet.noneOf(BloodGroup.class);
        }

        Set<BloodGroup> compatible = EnumSet.noneOf(BloodGroup.class);
        for (BloodGroup donorGroup : values()) {
            if (donorGroup.canDonateTo(recipientBloodGroup)) {
                compatible.add(donorGroup);
            }
        }
        return compatible;
    }

    /**
     * Convenience alias used by matching logic and REST-style compatibility lookups.
     */
    public static List<BloodGroup> getCompatibleDonorBloodGroups(BloodGroup recipientBloodGroup) {
        return new ArrayList<>(getCompatibleDonorGroups(recipientBloodGroup));
    }

    /**
     * Checks donor compatibility using the real blood-group rules.
     */
    public static boolean isCompatibleDonor(BloodGroup donorGroup, BloodGroup recipientBloodGroup) {
        return donorGroup != null && recipientBloodGroup != null && donorGroup.canDonateTo(recipientBloodGroup);
    }

    /**
     * Looks up a {@code BloodGroup} by its medical label string.
     *
     * @param label e.g. "A+" or "O-"
     * @return matching {@code BloodGroup}
     * @throws IllegalArgumentException if label is not recognised
     */
    @JsonCreator
    public static BloodGroup fromLabel(String label) {
        if (label == null || label.isBlank()) {
            return O_POSITIVE;
        }
        for (BloodGroup bg : values()) {
            if (bg.label.equalsIgnoreCase(label) || bg.name().equalsIgnoreCase(label)) {
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
