package com.lifelink.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lifelink.json.JsonDataService;
import com.lifelink.util.HaversineCalculator;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

public class HospitalApiService {
    private static final ObjectMapper OBJECT_MAPPER = JsonDataService.getObjectMapper();
    private static final int TIMEOUT_SECONDS = Integer.parseInt(ApiConfigLoader.get("http.timeout.seconds", "15"));
    private static final String OVERPASS_URL = ApiConfigLoader.get("overpass.url");
    private static final String USER_AGENT = ApiConfigLoader.get("overpass.user-agent", "LifeLink/1.0");

    private final HttpClient httpClient = HttpClient.newBuilder()
            .version(HttpClient.Version.HTTP_1_1)
            .connectTimeout(Duration.ofSeconds(TIMEOUT_SECONDS))
            .build();

    public List<HospitalDTO> searchNearbyHospitals(double latitude, double longitude, int limit) throws IOException, InterruptedException {
        if (!Double.isFinite(latitude) || !Double.isFinite(longitude)) {
            throw new IllegalArgumentException("A valid latitude and longitude are required.");
        }

        int maxResults = limit > 0 ? limit : Integer.parseInt(ApiConfigLoader.get("search.limit", "10"));
        maxResults = Math.max(1, Math.min(maxResults, 20));

        List<Integer> radii = List.of(5000, 15000, 25000);
        for (int radius : radii) {
            List<HospitalDTO> results = fetchHospitals(latitude, longitude, radius, maxResults);
            if (results != null && !results.isEmpty()) {
                return results;
            }
        }

        return List.of();
    }

    private List<HospitalDTO> fetchHospitals(double latitude, double longitude, int radiusMeters, int maxResults)
            throws IOException, InterruptedException {
        String query = "[out:json][timeout:25];("
                + "node[\"amenity\"~\"^(hospital|clinic|doctors)$\"](around:" + radiusMeters + "," + latitude + "," + longitude + ");"
                + "way[\"amenity\"~\"^(hospital|clinic|doctors)$\"](around:" + radiusMeters + "," + latitude + "," + longitude + ");"
                + "relation[\"amenity\"~\"^(hospital|clinic|doctors)$\"](around:" + radiusMeters + "," + latitude + "," + longitude + ");"
                + ");out center tags;";

        String formBody = "data=" + URLEncoder.encode(query, StandardCharsets.UTF_8);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(OVERPASS_URL))
                .header("User-Agent", USER_AGENT)
                .header("Accept", "application/json")
                .header("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8")
                .timeout(Duration.ofSeconds(TIMEOUT_SECONDS))
                .POST(HttpRequest.BodyPublishers.ofString(formBody))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() >= 400) {
            throw new IOException("Healthcare API request failed with HTTP status " + response.statusCode());
        }
        if (response.body() == null || response.body().isBlank()) {
            throw new IOException("Healthcare API returned an empty response.");
        }

        OverpassResponseDTO overpassResponse = OBJECT_MAPPER.readValue(response.body(), OverpassResponseDTO.class);
        if (overpassResponse == null || overpassResponse.getElements() == null || overpassResponse.getElements().isEmpty()) {
            return List.of();
        }

        return overpassResponse.getElements().stream()
                .filter(element -> element != null && Double.isFinite(element.getLatitude()) && Double.isFinite(element.getLongitude()))
                .map(element -> {
                    double distanceKm = HaversineCalculator.calculateDistanceKm(latitude, longitude, element.getLatitude(), element.getLongitude());
                    return new HospitalDTO(
                            element.getName(),
                            element.getAddress(),
                            element.getLatitude(),
                            element.getLongitude(),
                            distanceKm,
                            element.getType() != null ? element.getType() : "Health Facility"
                    );
                })
                .sorted(Comparator.comparingDouble(HospitalDTO::getDistanceKm))
                .limit(maxResults)
                .collect(Collectors.toList());
    }
}
