package com.remi481.sentinel.security;

import com.remi481.sentinel.exception.TooManyRequestsException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;

@Service
public class LoginThrottleService {

    private final Duration window;
    private final int maxUsernameFailures;
    private final int maxIpFailures;
    private final int maxEntries;
    private final Clock clock;

    private final Map<String, Deque<Instant>> usernameFailures = new LinkedHashMap<>();
    private final Map<String, Deque<Instant>> ipFailures = new LinkedHashMap<>();

    public static class Reservation {
        private final String username;
        private final String clientIp;
        private final Instant reservedAt;

        private Reservation(String username, String clientIp, Instant reservedAt) {
            this.username = username;
            this.clientIp = clientIp;
            this.reservedAt = reservedAt;
        }

        public String getUsername() {
            return username;
        }

        public String getClientIp() {
            return clientIp;
        }

        public Instant getReservedAt() {
            return reservedAt;
        }
    }

    public LoginThrottleService(
            @Value("${sentinel.security.throttle.window-minutes:15}") long windowMinutes,
            @Value("${sentinel.security.throttle.max-username-attempts:5}") int maxUsernameFailures,
            @Value("${sentinel.security.throttle.max-ip-attempts:20}") int maxIpFailures,
            @Value("${sentinel.security.throttle.max-entries:10000}") int maxEntries,
            Clock clock) {
        this.window = Duration.ofMinutes(windowMinutes);
        this.maxUsernameFailures = maxUsernameFailures;
        this.maxIpFailures = maxIpFailures;
        this.maxEntries = maxEntries;
        this.clock = clock;
    }

    public Reservation reserve(String username, String clientIp) {
        Instant now = Instant.now(clock);
        synchronized (this) {
            long usernameRetry = retryAfterSeconds(usernameFailures, username, maxUsernameFailures, now);
            long ipRetry = retryAfterSeconds(ipFailures, clientIp, maxIpFailures, now);
            long retryAfter = Math.max(usernameRetry, ipRetry);
            if (retryAfter > 0) {
                throw new TooManyRequestsException(retryAfter);
            }
            Reservation reservation = new Reservation(username, clientIp, now);
            addFailure(usernameFailures, username, now, maxUsernameFailures);
            addFailure(ipFailures, clientIp, now, maxIpFailures);
            return reservation;
        }
    }

    public void releaseSuccess(Reservation reservation) {
        if (reservation == null) {
            return;
        }
        synchronized (this) {
            if (reservation.username != null) {
                usernameFailures.remove(reservation.username);
            }
            if (reservation.clientIp != null && reservation.reservedAt != null) {
                removeSingleReservation(ipFailures, reservation.clientIp, reservation.reservedAt);
            }
        }
    }

    public void releaseFailure(Reservation reservation) {
        // The reservation recorded by reserve() stays as a failure.
    }

    int usernameEntryCount() {
        synchronized (this) {
            return usernameFailures.size();
        }
    }

    int ipEntryCount() {
        synchronized (this) {
            return ipFailures.size();
        }
    }

    private long retryAfterSeconds(Map<String, Deque<Instant>> failures, String key, int max, Instant now) {
        if (key == null) {
            return 0;
        }
        Deque<Instant> timestamps = failures.get(key);
        if (timestamps == null) {
            return 0;
        }
        prune(timestamps, now);
        int removeCount = timestamps.size() - max + 1;
        if (removeCount <= 0) {
            return 0;
        }
        Iterator<Instant> iterator = timestamps.iterator();
        Instant blockingExpiry = null;
        for (int i = 0; i < removeCount && iterator.hasNext(); i++) {
            blockingExpiry = iterator.next();
        }
        if (blockingExpiry == null) {
            return 0;
        }
        long seconds = Duration.between(now, blockingExpiry.plus(window)).getSeconds();
        return Math.max(1, seconds);
    }

    private void addFailure(Map<String, Deque<Instant>> failures, String key, Instant now, int maxAttempts) {
        if (key == null) {
            return;
        }
        Deque<Instant> timestamps = failures.computeIfAbsent(key, ignored -> new ArrayDeque<>());
        timestamps.addLast(now);
        prune(timestamps, now);
        evictIfFull(failures, maxAttempts);
    }

    private void prune(Deque<Instant> timestamps, Instant now) {
        Instant cutoff = now.minus(window);
        while (!timestamps.isEmpty() && !timestamps.peekFirst().isAfter(cutoff)) {
            timestamps.pollFirst();
        }
    }

    private void evictIfFull(Map<String, Deque<Instant>> failures, int maxAttempts) {
        // Only pay for the full sweep once the map has actually grown past its
        // bound; the common path just prunes the one deque it touched.
        if (failures.size() <= maxEntries) {
            return;
        }
        Instant now = Instant.now(clock);
        Iterator<Map.Entry<String, Deque<Instant>>> entryIterator = failures.entrySet().iterator();
        while (entryIterator.hasNext()) {
            Map.Entry<String, Deque<Instant>> entry = entryIterator.next();
            Deque<Instant> deque = entry.getValue();
            if (deque == null) {
                entryIterator.remove();
                continue;
            }
            prune(deque, now);
            if (deque.isEmpty()) {
                entryIterator.remove();
            }
        }
        while (failures.size() > maxEntries) {
            evictOne(failures, maxAttempts, now);
        }
    }

    private void evictOne(Map<String, Deque<Instant>> failures, int maxAttempts, Instant now) {
        Map.Entry<String, Deque<Instant>> leastRecentlyUpdatedUnblocked = null;
        Map.Entry<String, Deque<Instant>> blockedExpiringSoonest = null;
        Instant soonestExpiry = null;
        for (Map.Entry<String, Deque<Instant>> entry : failures.entrySet()) {
            Deque<Instant> deque = entry.getValue();
            if (deque == null || deque.isEmpty()) {
                continue;
            }
            if (deque.size() >= maxAttempts) {
                Instant blocking = blockingExpiry(deque, maxAttempts);
                if (blockedExpiringSoonest == null || blocking.isBefore(soonestExpiry)) {
                    blockedExpiringSoonest = entry;
                    soonestExpiry = blocking;
                }
                continue;
            }
            Instant last = deque.peekLast();
            if (leastRecentlyUpdatedUnblocked == null) {
                leastRecentlyUpdatedUnblocked = entry;
                continue;
            }
            Instant bestLast = leastRecentlyUpdatedUnblocked.getValue().peekLast();
            if (last != null && (bestLast == null || last.isBefore(bestLast))) {
                leastRecentlyUpdatedUnblocked = entry;
            }
        }
        Map.Entry<String, Deque<Instant>> victim =
                leastRecentlyUpdatedUnblocked != null ? leastRecentlyUpdatedUnblocked : blockedExpiringSoonest;
        if (victim == null) {
            Iterator<String> keys = failures.keySet().iterator();
            if (!keys.hasNext()) {
                return;
            }
            keys.next();
            keys.remove();
            return;
        }
        failures.remove(victim.getKey());
    }

    private Instant blockingExpiry(Deque<Instant> timestamps, int maxAttempts) {
        int removeCount = timestamps.size() - maxAttempts + 1;
        if (removeCount <= 0) {
            return Instant.MAX;
        }
        Instant blockingTimestamp = null;
        Iterator<Instant> iterator = timestamps.iterator();
        for (int i = 0; i < removeCount && iterator.hasNext(); i++) {
            blockingTimestamp = iterator.next();
        }
        if (blockingTimestamp == null) {
            return Instant.MAX;
        }
        return blockingTimestamp.plus(window);
    }

    private void removeSingleReservation(Map<String, Deque<Instant>> failures, String key, Instant reservedAt) {
        if (key == null || reservedAt == null) {
            return;
        }
        Deque<Instant> timestamps = failures.get(key);
        if (timestamps == null || timestamps.isEmpty()) {
            return;
        }
        boolean removed = timestamps.removeLastOccurrence(reservedAt);
        if (!removed) {
            Iterator<Instant> iterator = timestamps.descendingIterator();
            while (iterator.hasNext()) {
                Instant candidate = iterator.next();
                if (candidate != null && candidate.equals(reservedAt)) {
                    iterator.remove();
                    removed = true;
                    break;
                }
            }
        }
        if (timestamps.isEmpty()) {
            failures.remove(key);
        }
    }
}
