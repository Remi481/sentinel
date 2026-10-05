package com.remi481.sentinel.agent;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.time.Instant;
import java.util.UUID;

public class AgentMain {

    private static final String AGENT_VERSION = "0.1.0";

    public static void main(String[] args) throws Exception {
        System.out.println("Sentinel agent starting...");

        String baseUrl = System.getenv().getOrDefault("SENTINEL_API_URL", "http://localhost:8080");
        UUID deviceId = AgentIdentity.loadOrCreate();
        SentinelApiClient client = new SentinelApiClient(baseUrl);

        RegisterRequest registration = new RegisterRequest(
                deviceId,
                hostname(),
                System.getProperty("os.name"),
                System.getProperty("os.arch"),
                ipAddress(),
                AGENT_VERSION,
                "ONLINE",
                Instant.now().toString());

        int status = client.register(registration);
        System.out.println("Registered device " + deviceId + " (HTTP " + status + ")");
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