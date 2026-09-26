package com.lifelink.json;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class JsonApiResponse {
    private String status;
    private String message;
    private int matchedDonors;
    private int etaMinutes;

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public int getMatchedDonors() {
        return matchedDonors;
    }

    public void setMatchedDonors(int matchedDonors) {
        this.matchedDonors = matchedDonors;
    }

    public int getEtaMinutes() {
        return etaMinutes;
    }

    public void setEtaMinutes(int etaMinutes) {
        this.etaMinutes = etaMinutes;
    }
}
