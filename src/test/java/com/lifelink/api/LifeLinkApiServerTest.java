package com.lifelink.api;

import com.lifelink.database.DatabaseInitializer;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class LifeLinkApiServerTest {

    @Test
    void healthEndpointReturnsServerStatus() throws Exception {
        System.setProperty("lifelink.demo.data.enabled", "false");
        DatabaseInitializer.initialize();
        LifeLinkApiServer server = LifeLinkApiServer.start(0);

        try {
            HttpClient client = HttpClient.newHttpClient();
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("http://localhost:" + server.getPort() + "/api/health"))
                    .GET()
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            assertEquals(200, response.statusCode());
            assertNotNull(response.body());
            assertEquals("UP", response.body().toLowerCase().contains("up") ? "UP" : "DOWN");
        } finally {
            server.stop();
        }
    }
}
