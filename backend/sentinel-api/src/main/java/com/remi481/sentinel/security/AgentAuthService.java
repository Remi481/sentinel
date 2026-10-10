package com.remi481.sentinel.security;

import com.remi481.sentinel.entity.Device;
import com.remi481.sentinel.exception.UnauthorizedException;
import com.remi481.sentinel.repository.DeviceRepository;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

@Service
public class AgentAuthService {

    private static final String BEARER_PREFIX = "Bearer ";

    private final SecurityProperties properties;
    private final DeviceRepository deviceRepository;
    private final DeviceTokenService deviceTokenService;

    public AgentAuthService(SecurityProperties properties,
                            DeviceRepository deviceRepository,
                            DeviceTokenService deviceTokenService) {
        this.properties = properties;
        this.deviceRepository = deviceRepository;
        this.deviceTokenService = deviceTokenService;
    }

    public Device authenticateDeviceToken(String authorizationHeader) {
        String token = bearerToken(authorizationHeader);
        if (token == null) {
            throw new UnauthorizedException(UnauthorizedException.MESSAGE);
        }
        String tokenHash = deviceTokenService.hash(token);
        return deviceRepository.findByTokenHash(tokenHash)
                .orElseThrow(() -> new UnauthorizedException(UnauthorizedException.MESSAGE));
    }

    public void requireEnrollmentKey(String enrollmentKeyHeader) {
        String expected = properties.enrollmentKey();
        if (enrollmentKeyHeader == null
                || !constantTimeEquals(enrollmentKeyHeader, expected)) {
            throw new UnauthorizedException(UnauthorizedException.MESSAGE);
        }
    }

    private String bearerToken(String authorizationHeader) {
        if (authorizationHeader == null || !authorizationHeader.startsWith(BEARER_PREFIX)) {
            return null;
        }
        String token = authorizationHeader.substring(BEARER_PREFIX.length()).trim();
        return token.isEmpty() ? null : token;
    }

    private boolean constantTimeEquals(String first, String second) {
        return MessageDigest.isEqual(
                first.getBytes(StandardCharsets.UTF_8),
                second.getBytes(StandardCharsets.UTF_8));
    }
}