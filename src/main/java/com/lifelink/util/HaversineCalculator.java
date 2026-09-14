package com.lifelink.util;

/**
 * Provides geospatial distance calculations between two GPS coordinates.
 *
 * <p><b>Algorithm — Haversine Formula:</b>
 * The Haversine formula calculates the great-circle distance between two points
 * on a sphere given their latitude and longitude in degrees. This is more accurate
 * than a simple Euclidean distance calculation because it accounts for the
 * curvature of the Earth.
 *
 * <p>Formula:
 * <pre>
 *   a = sin²(Δlat/2) + cos(lat1) · cos(lat2) · sin²(Δlon/2)
 *   c = 2 · atan2(√a, √(1-a))
 *   d = R · c        (where R = 6371 km, Earth's mean radius)
 * </pre>
 *
 * <p>This is used by the donor matching engine and the "nearby" location searches
 * for blood banks and hospitals.
 *
 * <p><b>Package:</b> com.lifelink.util
 * <p><b>Used by:</b> LocationService, MatchingEngine
 */
public class HaversineCalculator {

    /** Mean radius of the Earth in kilometres. */
    private static final double EARTH_RADIUS_KM = 6371.0;

    /** Prevent instantiation — purely a utility class. */
    private HaversineCalculator() {}

    /**
     * Calculates the great-circle distance between two coordinate pairs.
     *
     * @param lat1 latitude of point 1 in decimal degrees
     * @param lon1 longitude of point 1 in decimal degrees
     * @param lat2 latitude of point 2 in decimal degrees
     * @param lon2 longitude of point 2 in decimal degrees
     * @return distance in kilometres (always non-negative)
     */
    public static double calculateDistanceKm(double lat1, double lon1,
                                             double lat2, double lon2) {
        // Convert degrees to radians
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);

        double radLat1 = Math.toRadians(lat1);
        double radLat2 = Math.toRadians(lat2);

        // Haversine formula
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                 + Math.cos(radLat1) * Math.cos(radLat2)
                 * Math.sin(dLon / 2) * Math.sin(dLon / 2);

        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));

        return EARTH_RADIUS_KM * c;
    }

    /**
     * Convenience overload that accepts {@link Double} wrappers (nullable).
     * Returns {@link Double#MAX_VALUE} if either coordinate is null,
     * which effectively places the point infinitely far away in sorted results.
     *
     * @param lat1 latitude of point 1 (nullable)
     * @param lon1 longitude of point 1 (nullable)
     * @param lat2 latitude of point 2 (nullable)
     * @param lon2 longitude of point 2 (nullable)
     * @return distance in km, or {@link Double#MAX_VALUE} if coordinates are unavailable
     */
    public static double calculateDistanceKm(Double lat1, Double lon1,
                                             Double lat2, Double lon2) {
        if (lat1 == null || lon1 == null || lat2 == null || lon2 == null) {
            return Double.MAX_VALUE;
        }
        return calculateDistanceKm((double) lat1, (double) lon1,
                                   (double) lat2, (double) lon2);
    }
}
