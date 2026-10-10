package com.remi481.sentinel.security;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;

@Component
public class SecurityPropertiesValidator implements InitializingBean {

    private final SecurityProperties properties;

    public SecurityPropertiesValidator(SecurityProperties properties) {
        this.properties = properties;
    }

    @Override
    public void afterPropertiesSet() {
        byte[] secret = properties.jwtSecret() == null ? new byte[0] : properties.jwtSecret().getBytes(StandardCharsets.UTF_8);
        if (secret.length < 32) {
            throw new IllegalStateException("sentinel.security.jwt-secret (SENTINEL_JWT_SECRET) must be a random value of at least 32 bytes. Generate one with: openssl rand -base64 32");
        }

        if (properties.enrollmentKey() == null || properties.enrollmentKey().length() < 24) {
            throw new IllegalStateException("sentinel.security.enrollment-key (SENTINEL_ENROLLMENT_KEY) must be a random value of at least 24 characters. Generate one with: openssl rand -base64 32");
        }

        boolean usernameSet = properties.adminUsername() != null && !properties.adminUsername().isBlank();
        boolean passwordSet = properties.adminPassword() != null && !properties.adminPassword().isBlank();
        if (usernameSet != passwordSet) {
            throw new IllegalStateException("sentinel.security.admin-username and admin-password (SENTINEL_ADMIN_USERNAME, SENTINEL_ADMIN_PASSWORD) must be set together");
        }
        if (passwordSet && properties.adminPassword().length() < 12) {
            throw new IllegalStateException("sentinel.security.admin-password (SENTINEL_ADMIN_PASSWORD) must be at least 12 characters");
        }
    }
}