package com.remi481.sentinel.agent;

import java.util.UUID;

public record RegisterRequest(
        UUID uuid,
        String hostname,
        String operatingSystem,
        String architecture,
        String ipAddress,
        String agentVersion,
        String status,
        String lastSeen
) {
}
