package com.lifelink.api;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

import java.io.IOException;

/**
 * DTO for a Nominatim geocoding result.
 *
 * <p>Nominatim returns {@code lat} and {@code lon} as JSON <em>strings</em>
 * (e.g. {@code "lat":"22.8440"}), not as numbers. Jackson would silently
 * leave them as 0.0 when mapped to a {@code double} field. A lightweight
 * custom deserializer handles both the string and numeric cases correctly.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class NominatimLocationDTO {

    @JsonProperty("lat")
    @JsonDeserialize(using = StringOrNumberDoubleDeserializer.class)
    private double latitude;

    @JsonProperty("lon")
    @JsonDeserialize(using = StringOrNumberDoubleDeserializer.class)
    private double longitude;

    @JsonProperty("display_name")
    private String displayName;

    @JsonProperty("type")
    private String type;

    public double getLatitude() { return latitude; }
    public void setLatitude(double latitude) { this.latitude = latitude; }

    public double getLongitude() { return longitude; }
    public void setLongitude(double longitude) { this.longitude = longitude; }

    public String getDisplayName() { return displayName; }
    public void setDisplayName(String displayName) { this.displayName = displayName; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    /**
     * Deserializes a value that Nominatim returns either as a JSON string
     * ({@code "22.8440"}) or as a JSON number ({@code 22.8440}).
     */
    public static class StringOrNumberDoubleDeserializer extends JsonDeserializer<Double> {
        @Override
        public Double deserialize(JsonParser p, DeserializationContext ctx) throws IOException {
            String text = p.getText();
            if (text == null || text.isBlank()) {
                return Double.NaN;
            }
            try {
                return Double.parseDouble(text.trim());
            } catch (NumberFormatException ex) {
                return Double.NaN;
            }
        }
    }
}
