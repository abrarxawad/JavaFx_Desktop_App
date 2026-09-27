package com.lifelink.api;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lifelink.json.JsonDataService;
import com.lifelink.model.BloodGroup;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

public class DirectoryApiService {
    private static final ObjectMapper OBJECT_MAPPER = JsonDataService.getObjectMapper();
    private static final String BASE_URL = "http://localhost:8080";

    private final HttpClient httpClient = HttpClient.newBuilder()
            .version(HttpClient.Version.HTTP_1_1)
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    public List<DirectoryEntry> fetchDonors() throws IOException, InterruptedException {
        return fetchDirectory("/api/donors");
    }

    public List<DirectoryEntry> fetchRecipients() throws IOException, InterruptedException {
        return fetchDirectory("/api/recipients");
    }

    private List<DirectoryEntry> fetchDirectory(String endpoint) throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + endpoint))
                .header("Accept", "application/json")
                .timeout(Duration.ofSeconds(10))
                .GET()
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() >= 400) {
            throw new IOException("Directory API request failed with status " + response.statusCode() + ": " + response.body());
        }

        JsonNode root = OBJECT_MAPPER.readTree(response.body());
        JsonNode items = root.path("items");
        if (!items.isArray()) {
            return List.of();
        }

        List<DirectoryEntry> entries = new ArrayList<>();
        for (JsonNode item : items) {
            if (item == null || item.isNull()) {
                continue;
            }

            DirectoryEntry entry = new DirectoryEntry();
            entry.setName(readName(item));
            entry.setBloodGroup(readBloodGroup(item));
            entry.setCity(readCity(item));
            entry.setPhone(readPhone(item));
            entry.setType(item.path("type").asText(endpoint.contains("recipients") ? "Recipient" : "Donor"));
            entries.add(entry);
        }
        return entries;
    }

    private String readName(JsonNode item) {
        String first = textOrBlank(item.path("firstName"));
        String last = textOrBlank(item.path("lastName"));
        if (first.isBlank() && last.isBlank()) {
            first = textOrBlank(item.path("first_name"));
            last = textOrBlank(item.path("last_name"));
        }

        String combined = (first + " " + last).trim();
        if (!combined.isBlank()) {
            return combined;
        }

        String directName = textOrBlank(item.path("name"));
        if (!directName.isBlank()) {
            return directName;
        }

        return "Unknown User";
    }

    private String readBloodGroup(JsonNode item) {
        String raw = firstNonBlank(
                textOrBlank(item.path("bloodGroup")),
                textOrBlank(item.path("blood_group")),
                textOrBlank(item.path("bloodGroupCode"))
        );
        if (raw.isBlank()) {
            return "Unknown";
        }
        try {
            return BloodGroup.fromLabel(raw).getLabel();
        } catch (IllegalArgumentException ex) {
            return raw;
        }
    }

    private String readCity(JsonNode item) {
        return firstNonBlank(
                textOrBlank(item.path("city")),
                textOrBlank(item.path("location").path("city")),
                "Unknown"
        );
    }

    private String readPhone(JsonNode item) {
        return firstNonBlank(
                textOrBlank(item.path("phone")),
                textOrBlank(item.path("phoneNumber")),
                textOrBlank(item.path("mobile")),
                "N/A"
        );
    }

    private String textOrBlank(JsonNode node) {
        return node == null || node.isNull() ? "" : node.asText(" ").trim();
    }

    private String firstNonBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        return "Unknown";
    }

    public static class DirectoryEntry {
        private String name;
        private String bloodGroup;
        private String city;
        private String phone;
        private String type;

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getBloodGroup() {
            return bloodGroup;
        }

        public void setBloodGroup(String bloodGroup) {
            this.bloodGroup = bloodGroup;
        }

        public String getCity() {
            return city;
        }

        public void setCity(String city) {
            this.city = city;
        }

        public String getPhone() {
            return phone;
        }

        public void setPhone(String phone) {
            this.phone = phone;
        }

        public String getType() {
            return type;
        }

        public void setType(String type) {
            this.type = type;
        }
    }
}
