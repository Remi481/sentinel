package com.remi481.sentinel.agent;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

public class SentinelApiClient {

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .build();
    private final ObjectMapper mapper = new ObjectMapper();
    private final String baseUrl;

    public SentinelApiClient(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    public int register(RegisterRequest request) throws IOException, InterruptedException {
        return post("/api/v1/devices/register", request);
    }

    private int post(String path, Object body) throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + path))
                .timeout(Duration.ofSeconds(10))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(mapper.writeValueAsString(body)))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        return response.statusCode();
    }
}

