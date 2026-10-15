package com.remi481.sentinel.security;

import com.remi481.sentinel.exception.TooManyRequestsException;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LoginThrottleServiceTest {

    private static final long WINDOW_MINUTES = 15;
    private static final int MAX_USERNAME_ATTEMPTS = 5;
    private static final int MAX_IP_ATTEMPTS = 20;

    private final MutableClock clock = new MutableClock();
    private final LoginThrottleService service = new LoginThrottleService(
            WINDOW_MINUTES, MAX_USERNAME_ATTEMPTS, MAX_IP_ATTEMPTS, 10000, clock);

    @Test
    void blocksUsernameAfterFiveFailures() {
        for (int attempt = 0; attempt < MAX_USERNAME_ATTEMPTS; attempt++) {
            service.recordFailure("alice", "10.0.0.1");
        }
        assertThrows(TooManyRequestsException.class, () -> service.checkAllowed("alice", "10.0.0.2"));
    }

    @Test
    void anotherUsernameFromTheSameIpIsNotAffectedByUsernameLimit() {
        for (int attempt = 0; attempt < MAX_USERNAME_ATTEMPTS; attempt++) {
            service.recordFailure("alice", "10.0.0.1");
        }
        assertDoesNotThrow(() -> service.checkAllowed("bob", "10.0.0.1"));
    }

    @Test
    void blocksIpAfterTwentyFailuresAcrossUsernames() {
        for (int attempt = 0; attempt < MAX_IP_ATTEMPTS; attempt++) {
            service.recordFailure("user-" + attempt, "10.0.0.9");
        }
        assertThrows(TooManyRequestsException.class, () -> service.checkAllowed("another-user", "10.0.0.9"));
    }

    @Test
    void successResetsUsernameCounterButNotIpCounter() {
        for (int attempt = 0; attempt < MAX_USERNAME_ATTEMPTS; attempt++) {
            service.recordFailure("alice", "10.0.0.1");
        }
        assertThrows(TooManyRequestsException.class, () -> service.checkAllowed("alice", "10.0.0.1"));

        service.recordSuccess("alice");

        assertDoesNotThrow(() -> service.checkAllowed("alice", "10.0.0.1"));
    }

    @Test
    void successDoesNotResetIpCounter() {
        for (int attempt = 0; attempt < MAX_IP_ATTEMPTS; attempt++) {
            service.recordFailure("user-" + attempt, "10.0.0.9");
        }
        service.recordSuccess("user-0");
        assertThrows(TooManyRequestsException.class, () -> service.checkAllowed("fresh-user", "10.0.0.9"));
    }

    @Test
    void blockExpiresAfterTheWindow() {
        for (int attempt = 0; attempt < MAX_USERNAME_ATTEMPTS; attempt++) {
            service.recordFailure("alice", "10.0.0.1");
        }
        assertThrows(TooManyRequestsException.class, () -> service.checkAllowed("alice", "10.0.0.1"));

        clock.advance(Duration.ofMinutes(WINDOW_MINUTES).plusSeconds(1));

        assertDoesNotThrow(() -> service.checkAllowed("alice", "10.0.0.1"));
    }

    @Test
    void retryAfterIsPositiveAndWithinTheWindow() {
        for (int attempt = 0; attempt < MAX_USERNAME_ATTEMPTS; attempt++) {
            service.recordFailure("alice", "10.0.0.1");
            clock.advance(Duration.ofSeconds(1));
        }

        TooManyRequestsException exception =
                assertThrows(TooManyRequestsException.class, () -> service.checkAllowed("alice", "10.0.0.2"));
        assertTrue(exception.getRetryAfterSeconds() > 0);
        assertTrue(exception.getRetryAfterSeconds() <= Duration.ofMinutes(WINDOW_MINUTES).toSeconds());
        assertEquals(TooManyRequestsException.MESSAGE, exception.getMessage());
    }

    @Test
    void storeIsBoundedByMaxEntries() {
        LoginThrottleService bounded = new LoginThrottleService(
                WINDOW_MINUTES, MAX_USERNAME_ATTEMPTS, MAX_IP_ATTEMPTS, 3, clock);
        for (int attempt = 0; attempt < 50; attempt++) {
            bounded.recordFailure("user-" + attempt, "ip-" + attempt);
        }
        assertTrue(bounded.usernameEntryCount() <= 3);
        assertTrue(bounded.ipEntryCount() <= 3);
    }

    private static final class MutableClock extends Clock {

        private Instant now = Instant.parse("2026-01-01T00:00:00Z");

        @Override
        public ZoneId getZone() {
            return ZoneId.of("UTC");
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return now;
        }

        void advance(Duration duration) {
            now = now.plus(duration);
        }
    }
}
