package com.remi481.sentinel.agent;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.UUID;

@JsonIgnoreProperties(ignoreUnknown = true)
public record RegisterResponse(DeviceInfo device, String deviceToken) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record DeviceInfo(
            UUID uuid,
            String hostname,
            String operatingSystem,
            String architecture,
            String ipAddress,
            String agentVersion,
            String status,
            String lastSeen) {
    }
}
