package com.lifelink.api;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lifelink.json.JsonDataService;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

public class LocationApiService {
    private static final ObjectMapper OBJECT_MAPPER = JsonDataService.getObjectMapper();
    private static final int TIMEOUT_SECONDS = Integer.parseInt(ApiConfigLoader.get("http.timeout.seconds", "15"));
    private static final String NOMINATIM_URL = ApiConfigLoader.get("nominatim.url");
    private static final String USER_AGENT = ApiConfigLoader.get("nominatim.user-agent", "LifeLink/1.0");

    private final HttpClient httpClient = HttpClient.newBuilder()
            .version(HttpClient.Version.HTTP_1_1)
            .connectTimeout(Duration.ofSeconds(TIMEOUT_SECONDS))
            .build();

    public NominatimLocationDTO geocodeLocation(String query) throws IOException, InterruptedException {
        if (query == null || query.isBlank()) {
            throw new IllegalArgumentException("Please enter a valid location.");
        }

        String encodedQuery = URLEncoder.encode(query.trim(), StandardCharsets.UTF_8);
        String url = NOMINATIM_URL + "?q=" + encodedQuery + "&format=json&limit=1";

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("User-Agent", USER_AGENT)
                .header("Accept", "application/json")
                .timeout(Duration.ofSeconds(TIMEOUT_SECONDS))
                .GET()
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() >= 400) {
            throw new IOException("Nominatim request failed with HTTP status " + response.statusCode());
        }

        if (response.body() == null || response.body().isBlank()) {
            throw new IOException("Nominatim returned an empty response.");
        }

        NominatimLocationDTO[] matches = OBJECT_MAPPER.readValue(response.body(), new TypeReference<NominatimLocationDTO[]>() {});
        if (matches == null || matches.length == 0) {
            throw new IllegalArgumentException("No matching location found for: " + query);
        }

        NominatimLocationDTO result = matches[0];
        if (Double.isNaN(result.getLatitude()) || Double.isNaN(result.getLongitude())) {
            throw new IllegalArgumentException("The selected location does not contain valid coordinates.");
        }
        return result;
    }
}
