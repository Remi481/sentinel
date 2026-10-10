package com.remi481.sentinel.security;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "sentinel.security")
public record SecurityProperties(
        String jwtSecret,
        long jwtTtlSeconds,
        String enrollmentKey,
        String adminUsername,
        String adminPassword) {
}