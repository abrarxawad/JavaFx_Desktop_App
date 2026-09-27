package com.lifelink.api;

import com.lifelink.dao.DonorDAO;
import com.lifelink.model.BloodGroup;
import com.lifelink.model.Donor;

import java.io.IOException;
import java.util.List;

/**
 * Search service for compatible donor matching.
 *
 * <p>The source of truth remains SQLite through {@link DonorDAO}. This service only
 * coordinates the request: it resolves the search location with the external geocoder
 * (when provided) and then delegates the actual filtered lookup to the DAO layer.
 */
public class DonorSearchApiService {
    private final DonorDAO donorDAO = new DonorDAO();
    private final LocationApiService locationApiService = new LocationApiService();

    public List<Donor> searchCompatibleDonors(String bloodGroupText, String locationQuery, double radiusKm)
            throws IOException, InterruptedException {
        BloodGroup targetGroup = BloodGroup.fromLabel(bloodGroupText);

        if (locationQuery == null || locationQuery.isBlank()) {
            return donorDAO.findCompatibleDonors(targetGroup, null, null, null);
        }

        NominatimLocationDTO geocode = locationApiService.geocodeLocation(locationQuery);
        return donorDAO.findCompatibleDonors(targetGroup, geocode.getLatitude(), geocode.getLongitude(), radiusKm);
    }
}
