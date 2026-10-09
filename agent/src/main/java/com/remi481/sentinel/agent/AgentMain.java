package com.remi481.sentinel.agent;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.time.Instant;
import java.util.UUID;

public class AgentMain {

    private static final String AGENT_VERSION = "0.1.0";

    public static void main(String[] args) throws Exception {
        System.out.println("Sentinel agent starting...");

        String baseUrl = System.getenv().getOrDefault("SENTINEL_API_URL", "http://localhost:8080");
        long intervalSeconds = Long.parseLong(
                System.getenv().getOrDefault("SENTINEL_INTERVAL_SECONDS", "30"));

        UUID deviceId = AgentIdentity.loadOrCreate();
        SentinelApiClient client = new SentinelApiClient(baseUrl);
        SystemMetricsCollector collector = new SystemMetricsCollector();

        if (args.length > 0 && args[0].equals("--print")) {
            Thread.sleep(1000);
            System.out.println(collector.collect());
            return;
        }

        registerUntilSuccessful(client, deviceId, intervalSeconds);

        while (true) {
            Thread.sleep(intervalSeconds * 1000);
            try {
                MetricsSnapshot snapshot = collector.collect();
                int metricsStatus = client.sendMetrics(MetricsRequest.of(deviceId, snapshot));
                int heartbeatStatus = client.sendHeartbeat(new HeartbeatRequest(deviceId));
                System.out.println(Instant.now() + " metrics HTTP " + metricsStatus
                        + ", heartbeat HTTP " + heartbeatStatus);
            } catch (IOException e) {
                System.out.println("Could not reach the API: " + e);
            }
        }
    }

    private static void registerUntilSuccessful(SentinelApiClient client, UUID deviceId,
                                                long intervalSeconds) throws InterruptedException {
        while (true) {
            try {
                int status = client.register(buildRegistration(deviceId));
                if (status >= 200 && status < 300) {
                    System.out.println("Registered device " + deviceId + " (HTTP " + status + ")");
                    return;
                }
                System.out.println("Registration failed (HTTP " + status + "), retrying...");
            } catch (IOException e) {
                System.out.println("Could not reach the API: " + e + ", retrying...");
            }
            Thread.sleep(intervalSeconds * 1000);
        }
    }

    private static RegisterRequest buildRegistration(UUID deviceId) {
        return new RegisterRequest(
                deviceId,
                hostname(),
                System.getProperty("os.name"),
                System.getProperty("os.arch"),
                ipAddress(),
                AGENT_VERSION,
                "ONLINE",
                Instant.now().toString());
    }

    private static String hostname() {
        try {
            return InetAddress.getLocalHost().getHostName();
        } catch (UnknownHostException e) {
            return "unknown";
        }
    }

    private static String ipAddress() {
        try {
            return InetAddress.getLocalHost().getHostAddress();
        } catch (UnknownHostException e) {
            return null;
        }
    }
}