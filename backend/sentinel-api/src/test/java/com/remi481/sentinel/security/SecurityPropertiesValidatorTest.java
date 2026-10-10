package com.remi481.sentinel.security;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class SecurityPropertiesValidatorTest {

    private static final String VALID_SECRET = "abcdefghijklmnopqrstuvwxyz123456";
    private static final String VALID_KEY = "enrollment-key-1234567890abcdef";
    private static final String VALID_USERNAME = "admin";
    private static final String VALID_PASSWORD = "admin-password-1234";

    private SecurityProperties properties(String secret, String key, String username, String password) {
        return new SecurityProperties(secret, 3600, key, username, password);
    }

    private void validate(SecurityProperties properties) {
        new SecurityPropertiesValidator(properties).afterPropertiesSet();
    }

    @Test
    void validPropertiesPass() {
        assertDoesNotThrow(() -> validate(properties(VALID_SECRET, VALID_KEY, VALID_USERNAME, VALID_PASSWORD)));
    }

    @Test
    void missingSecretIsRejected() {
        assertThrows(IllegalStateException.class, () -> validate(properties("", VALID_KEY, VALID_USERNAME, VALID_PASSWORD)));
    }

    @Test
    void shortSecretIsRejected() {
        assertThrows(IllegalStateException.class, () -> validate(properties("too-short", VALID_KEY, VALID_USERNAME, VALID_PASSWORD)));
    }

    @Test
    void shortEnrollmentKeyIsRejected() {
        assertThrows(IllegalStateException.class, () -> validate(properties(VALID_SECRET, "short", VALID_USERNAME, VALID_PASSWORD)));
    }

    @Test
    void mismatchedAdminCredentialsAreRejected() {
        assertThrows(IllegalStateException.class, () -> validate(properties(VALID_SECRET, VALID_KEY, VALID_USERNAME, "")));
        assertThrows(IllegalStateException.class, () -> validate(properties(VALID_SECRET, VALID_KEY, "", VALID_PASSWORD)));
    }

    @Test
    void shortAdminPasswordIsRejected() {
        assertThrows(IllegalStateException.class, () -> validate(properties(VALID_SECRET, VALID_KEY, VALID_USERNAME, "short")));
    }

    @Test
    void noAdminConfiguredIsAllowed() {
        assertDoesNotThrow(() -> validate(properties(VALID_SECRET, VALID_KEY, "", "")));
    }
}