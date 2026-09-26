package com.lifelink.api;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@JsonIgnoreProperties(ignoreUnknown = true)
public class OverpassResponseDTO {

    private List<Element> elements = new ArrayList<>();

    @JsonProperty("elements")
    public List<Element> getElements() {
        return elements;
    }

    @JsonProperty("elements")
    public void setElements(List<Element> elements) {
        this.elements = elements;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Element {
        private long id;
        private String type;
        private Double lat;
        private Double lon;
        private Center center;
        private Map<String, String> tags = new HashMap<>();

        public long getId() {
            return id;
        }

        public void setId(long id) {
            this.id = id;
        }

        public String getType() {
            return type;
        }

        public void setType(String type) {
            this.type = type;
        }

        public Double getLat() {
            return lat;
        }

        public void setLat(Double lat) {
            this.lat = lat;
        }

        public Double getLon() {
            return lon;
        }

        public void setLon(Double lon) {
            this.lon = lon;
        }

        public Center getCenter() {
            return center;
        }

        public void setCenter(Center center) {
            this.center = center;
        }

        @JsonProperty("tags")
        public Map<String, String> getTags() {
            return tags;
        }

        @JsonProperty("tags")
        public void setTags(Map<String, String> tags) {
            this.tags = tags;
        }

        public double getLatitude() {
            if (lat != null) {
                return lat;
            }
            return center != null ? center.getLat() : Double.NaN;
        }

        public double getLongitude() {
            if (lon != null) {
                return lon;
            }
            return center != null ? center.getLon() : Double.NaN;
        }

        public String getName() {
            String name = tags.get("name");
            if (name != null && !name.isBlank()) {
                return name;
            }
            return tags.getOrDefault("operator", "Healthcare Facility");
        }

        public String getAddress() {
            StringBuilder address = new StringBuilder();
            String street = tags.get("addr:street");
            String city = tags.get("addr:city");
            String town = tags.get("addr:town");
            String state = tags.get("addr:state");
            String country = tags.get("addr:country");

            if (street != null && !street.isBlank()) {
                address.append(street);
            }
            if (city != null && !city.isBlank()) {
                if (!address.isEmpty()) {
                    address.append(", ");
                }
                address.append(city);
            } else if (town != null && !town.isBlank()) {
                if (!address.isEmpty()) {
                    address.append(", ");
                }
                address.append(town);
            }
            if (state != null && !state.isBlank()) {
                if (!address.isEmpty()) {
                    address.append(", ");
                }
                address.append(state);
            }
            if (country != null && !country.isBlank()) {
                if (!address.isEmpty()) {
                    address.append(", ");
                }
                address.append(country);
            }
            return address.length() > 0 ? address.toString() : "Nearby healthcare facility";
        }

        @JsonIgnoreProperties(ignoreUnknown = true)
        public static class Center {
            private double lat;
            private double lon;

            public double getLat() {
                return lat;
            }

            public void setLat(double lat) {
                this.lat = lat;
            }

            public double getLon() {
                return lon;
            }

            public void setLon(double lon) {
                this.lon = lon;
            }
        }
    }
}
