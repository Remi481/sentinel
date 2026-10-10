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

    public RegistrationResult register(RegisterRequest request, String bearerToken, String enrollmentKey)
            throws IOException, InterruptedException {
        HttpRequest.Builder builder = baseRequest("/api/v1/devices/register");
        if (bearerToken != null) {
            builder.header("Authorization", "Bearer " + bearerToken);
        } else if (enrollmentKey != null) {
            builder.header("X-Enrollment-Key", enrollmentKey);
        }
        HttpResponse<String> response = send(builder, request);

        String deviceToken = null;
        if (response.statusCode() >= 200 && response.statusCode() < 300) {
            deviceToken = parseDeviceToken(response.body());
        }
        return new RegistrationResult(response.statusCode(), deviceToken);
    }

    public int sendMetrics(MetricsRequest request, String bearerToken)
            throws IOException, InterruptedException {
        return send(authenticatedRequest("/api/v1/metrics", bearerToken), request).statusCode();
    }

    public int sendHeartbeat(HeartbeatRequest request, String bearerToken)
            throws IOException, InterruptedException {
        return send(authenticatedRequest("/api/v1/devices/heartbeat", bearerToken), request).statusCode();
    }

    private HttpRequest.Builder baseRequest(String path) {
        return HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + path))
                .timeout(Duration.ofSeconds(10))
                .header("Content-Type", "application/json");
    }

    private HttpRequest.Builder authenticatedRequest(String path, String bearerToken) {
        HttpRequest.Builder builder = baseRequest(path);
        if (bearerToken != null) {
            builder.header("Authorization", "Bearer " + bearerToken);
        }
        return builder;
    }

    private HttpResponse<String> send(HttpRequest.Builder builder, Object body)
            throws IOException, InterruptedException {
        HttpRequest request = builder
                .POST(HttpRequest.BodyPublishers.ofString(mapper.writeValueAsString(body)))
                .build();
        return httpClient.send(request, HttpResponse.BodyHandlers.ofString());
    }

    private String parseDeviceToken(String body) {
        try {
            RegisterResponse parsed = mapper.readValue(body, RegisterResponse.class);
            return parsed.deviceToken();
        } catch (IOException e) {
            return null;
        }
    }

    public record RegistrationResult(int status, String deviceToken) {
    }
}
