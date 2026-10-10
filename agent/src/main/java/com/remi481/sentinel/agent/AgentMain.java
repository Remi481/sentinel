package com.remi481.sentinel.agent;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.time.Instant;
import java.util.UUID;

public class AgentMain {

    private static final String AGENT_VERSION = "0.1.0";

    private static final String TOKEN_REJECTED_MESSAGE =
            "The API rejected this device's token. It may have been revoked. "
                    + "Delete ~/.sentinel-agent/device-token and enrol again";

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

        DeviceTokenStore tokenStore = new DeviceTokenStore();
        String token = tokenStore.read();

        if (token == null) {
            String enrollmentKey = System.getenv("SENTINEL_ENROLLMENT_KEY");
            if (enrollmentKey == null || enrollmentKey.isBlank()) {
                System.out.println("SENTINEL_ENROLLMENT_KEY is required for first run. "
                        + "Set it to the API's enrolment key and start the agent again.");
                System.exit(1);
            }
            token = registerWithKeyUntilSuccessful(client, deviceId, enrollmentKey, intervalSeconds, tokenStore);
        } else {
            token = registerWithTokenUntilSuccessful(client, deviceId, token, intervalSeconds, tokenStore);
        }

        boolean tokenRejected = false;
        while (true) {
            Thread.sleep(intervalSeconds * 1000);
            try {
                MetricsSnapshot snapshot = collector.collect();
                int metricsStatus = client.sendMetrics(MetricsRequest.of(deviceId, snapshot), token);
                int heartbeatStatus = client.sendHeartbeat(new HeartbeatRequest(deviceId), token);

                if (isRejected(metricsStatus) || isRejected(heartbeatStatus)) {
                    if (!tokenRejected) {
                        System.out.println(TOKEN_REJECTED_MESSAGE);
                        tokenRejected = true;
                    }
                } else {
                    tokenRejected = false;
                    System.out.println(Instant.now() + " metrics HTTP " + metricsStatus
                            + ", heartbeat HTTP " + heartbeatStatus);
                }
            } catch (IOException e) {
                System.out.println("Could not reach the API: " + e);
            }
        }
    }

    private static String registerWithKeyUntilSuccessful(SentinelApiClient client, UUID deviceId,
                                                         String enrollmentKey, long intervalSeconds,
                                                         DeviceTokenStore tokenStore) throws InterruptedException {
        boolean warned = false;
        while (true) {
            try {
                SentinelApiClient.RegistrationResult result =
                        client.register(buildRegistration(deviceId), null, enrollmentKey);
                if (isSuccess(result.status())) {
                    saveToken(result, tokenStore);
                    System.out.println("Enrolled device " + deviceId + " (HTTP " + result.status() + ")");
                    return tokenStore.read();
                }
                if (isRejected(result.status())) {
                    if (!warned) {
                        System.out.println("The API rejected the enrolment. "
                                + "Check that SENTINEL_ENROLLMENT_KEY is correct.");
                        warned = true;
                    }
                } else {
                    warned = false;
                    System.out.println("Registration failed (HTTP " + result.status() + "), retrying...");
                }
            } catch (IOException e) {
                System.out.println("Could not reach the API: " + e + ", retrying...");
            }
            Thread.sleep(intervalSeconds * 1000);
        }
    }

    private static String registerWithTokenUntilSuccessful(SentinelApiClient client, UUID deviceId,
                                                           String token, long intervalSeconds,
                                                           DeviceTokenStore tokenStore) throws InterruptedException {
        boolean warned = false;
        while (true) {
            try {
                SentinelApiClient.RegistrationResult result =
                        client.register(buildRegistration(deviceId), token, null);
                if (isSuccess(result.status())) {
                    saveToken(result, tokenStore);
                    System.out.println("Re-registered device " + deviceId + " (HTTP " + result.status() + ")");
                    return tokenStore.read();
                }
                if (isRejected(result.status())) {
                    if (!warned) {
                        System.out.println(TOKEN_REJECTED_MESSAGE);
                        warned = true;
                    }
                } else {
                    warned = false;
                    System.out.println("Registration failed (HTTP " + result.status() + "), retrying...");
                }
            } catch (IOException e) {
                System.out.println("Could not reach the API: " + e + ", retrying...");
            }
            Thread.sleep(intervalSeconds * 1000);
        }
    }

    private static void saveToken(SentinelApiClient.RegistrationResult result, DeviceTokenStore tokenStore)
            throws IOException {
        if (result.deviceToken() != null) {
            tokenStore.write(result.deviceToken());
        }
    }

    private static boolean isSuccess(int status) {
        return status >= 200 && status < 300;
    }

    private static boolean isRejected(int status) {
        return status == 401 || status == 403;
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
