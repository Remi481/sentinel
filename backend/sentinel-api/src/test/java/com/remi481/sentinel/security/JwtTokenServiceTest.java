package com.remi481.sentinel.security;

import com.nimbusds.jose.jwk.source.ImmutableSecret;
import com.remi481.sentinel.entity.AppUser;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;

import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JwtTokenServiceTest {

    private static final String SECRET = "abcdefghijklmnopqrstuvwxyz123456";
    private static final long TTL_SECONDS = 3600;

    private final SecurityProperties properties = new SecurityProperties(
            SECRET, TTL_SECONDS, "enrollment-key-1234567890abcdef", "admin", "admin-password-1234");

    private JwtTokenService newService() {
        JwtEncoder encoder = new NimbusJwtEncoder(new ImmutableSecret<>(SECRET.getBytes(StandardCharsets.UTF_8)));
        return new JwtTokenService(encoder, properties);
    }

    private JwtDecoder decoder() {
        return NimbusJwtDecoder.withSecretKey(new SecretKeySpec(SECRET.getBytes(StandardCharsets.UTF_8), "HmacSHA256"))
                .macAlgorithm(MacAlgorithm.HS256)
                .build();
    }

    @Test
    void createsTokenWithSubjectRoleAndExpiry() {
        AppUser user = new AppUser("alice", "irrelevant-hash", "ADMIN", Instant.now());

        String token = newService().createToken(user);

        assertFalse(token.isBlank());
        Jwt jwt = decoder().decode(token);
        assertEquals("alice", jwt.getSubject());
        assertEquals("ADMIN", jwt.getClaimAsString("role"));
        assertNotNull(jwt.getIssuedAt());
        assertNotNull(jwt.getExpiresAt());
        assertTrue(jwt.getExpiresAt().isAfter(jwt.getIssuedAt()));
    }

    @Test
    void expiresInSecondsReturnsConfiguredTtl() {
        assertEquals(TTL_SECONDS, newService().expiresInSeconds());
    }
}