package com.remi481.sentinel.security;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class DeviceTokenServiceTest {

    @Test
    void generateAndHashProduceMatchingHash() {
        DeviceTokenService service = new DeviceTokenService();
        String token = service.generateDeviceToken();
        assertFalse(token.isBlank());
        String hash = service.hash(token);
        assertFalse(hash.isBlank());
        assertEquals(64, hash.length());
        assertEquals(hash, service.hash(token));
    }
}