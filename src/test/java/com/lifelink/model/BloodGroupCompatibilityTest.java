package com.lifelink.model;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class BloodGroupCompatibilityTest {

    @Test
    void oNegativeCanDonateToEveryBloodGroup() {
        assertTrue(BloodGroup.O_NEGATIVE.canDonateTo(BloodGroup.AB_POSITIVE));
        assertTrue(BloodGroup.O_NEGATIVE.canDonateTo(BloodGroup.O_NEGATIVE));
        assertTrue(BloodGroup.O_NEGATIVE.canDonateTo(BloodGroup.B_POSITIVE));
    }

    @Test
    void aPositiveMatchesOnlyCompatibleRecipients() {
        assertTrue(BloodGroup.A_POSITIVE.canDonateTo(BloodGroup.A_POSITIVE));
        assertTrue(BloodGroup.A_POSITIVE.canDonateTo(BloodGroup.AB_POSITIVE));
        assertFalse(BloodGroup.A_POSITIVE.canDonateTo(BloodGroup.B_POSITIVE));
    }

    @Test
    void compatibleDonorGroupsForAbPositiveUsesMedicalRules() {
        Set<BloodGroup> groups = BloodGroup.getCompatibleDonorGroups(BloodGroup.AB_POSITIVE);
        assertTrue(groups.contains(BloodGroup.A_POSITIVE));
        assertTrue(groups.contains(BloodGroup.B_POSITIVE));
        assertTrue(groups.contains(BloodGroup.AB_POSITIVE));
        assertTrue(groups.contains(BloodGroup.O_NEGATIVE));
        assertFalse(groups.contains(BloodGroup.O_POSITIVE));
    }

    @Test
    void donorRefreshEligibilityUsesLastDonationDate() {
        Donor donor = new Donor();
        donor.setAvailable(true);
        donor.setLastDonationDate(LocalDate.now().minusDays(30));
        donor.refreshEligibilityStatus();
        assertEquals("INELIGIBLE", donor.getEligibilityStatus());

        donor.setLastDonationDate(LocalDate.now().minusDays(90));
        donor.refreshEligibilityStatus();
        assertEquals("ELIGIBLE", donor.getEligibilityStatus());
    }
}
