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

    public void checkAllowed(String username, String clientIp) {
        Instant now = Instant.now(clock);
        synchronized (this) {
            long usernameRetry = retryAfterSeconds(usernameFailures, username, maxUsernameFailures, now);
            long ipRetry = retryAfterSeconds(ipFailures, clientIp, maxIpFailures, now);
            long retryAfter = Math.max(usernameRetry, ipRetry);
            if (retryAfter > 0) {
                throw new TooManyRequestsException(retryAfter);
            }
        }
    }

    public void recordFailure(String username, String clientIp) {
        Instant now = Instant.now(clock);
        synchronized (this) {
            addFailure(usernameFailures, username, now);
            addFailure(ipFailures, clientIp, now);
        }
    }

    public void recordSuccess(String username) {
        synchronized (this) {
            usernameFailures.remove(username);
        }
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

    private void addFailure(Map<String, Deque<Instant>> failures, String key, Instant now) {
        if (key == null) {
            return;
        }
        Deque<Instant> timestamps = failures.computeIfAbsent(key, ignored -> new ArrayDeque<>());
        timestamps.addLast(now);
        prune(timestamps, now);
        evictOldestIfFull(failures);
    }

    private void prune(Deque<Instant> timestamps, Instant now) {
        Instant cutoff = now.minus(window);
        while (!timestamps.isEmpty() && !timestamps.peekFirst().isAfter(cutoff)) {
            timestamps.pollFirst();
        }
    }

    private void evictOldestIfFull(Map<String, Deque<Instant>> failures) {
        while (failures.size() > maxEntries) {
            Iterator<String> keys = failures.keySet().iterator();
            if (!keys.hasNext()) {
                return;
            }
            keys.next();
            keys.remove();
        }
    }
}
