package com.remi481.sentinel;

import com.remi481.sentinel.entity.AppUser;
import com.remi481.sentinel.exception.TooManyRequestsException;
import com.remi481.sentinel.repository.AppUserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class LoginThrottleIntegrationTest {

    private static final String USERNAME_LIMIT_IP = "203.0.113.10";
    private static final String VICTIM_IP = "203.0.113.11";
    private static final String IP_LIMIT_ADDRESS = "198.51.100.7";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private AppUserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Test
    void sixthWrongPasswordForTheSameUsernameReturnsTooManyRequests() throws Exception {
        for (int attempt = 0; attempt < 5; attempt++) {
            attemptLogin("throttle-user-one", "wrong-password-" + attempt, USERNAME_LIMIT_IP)
                    .andExpect(status().isUnauthorized());
        }

        attemptLogin("throttle-user-one", "wrong-password-again", USERNAME_LIMIT_IP)
                .andExpect(status().isTooManyRequests())
                .andExpect(header().string("Retry-After", org.hamcrest.Matchers.not(org.hamcrest.Matchers.emptyString())))
                .andExpect(content().string(TooManyRequestsException.MESSAGE));
    }

    @Test
    void correctPasswordIsAlsoBlockedAfterFiveFailures() throws Exception {
        ensureUser("throttle-victim", "victim-password-1234");

        for (int attempt = 0; attempt < 5; attempt++) {
            attemptLogin("throttle-victim", "wrong-password-" + attempt, VICTIM_IP)
                    .andExpect(status().isUnauthorized());
        }

        attemptLogin("throttle-victim", "victim-password-1234", VICTIM_IP)
                .andExpect(status().isTooManyRequests())
                .andExpect(content().string(TooManyRequestsException.MESSAGE));
    }

    @Test
    void twentyFirstFailureFromTheSameIpReturnsTooManyRequests() throws Exception {
        for (int attempt = 0; attempt < 20; attempt++) {
            attemptLogin("throttle-ip-user-" + attempt, "wrong-password-" + attempt, IP_LIMIT_ADDRESS)
                    .andExpect(status().isUnauthorized());
        }

        attemptLogin("throttle-ip-user-extra", "wrong-password", IP_LIMIT_ADDRESS)
                .andExpect(status().isTooManyRequests())
                .andExpect(header().string("Retry-After", org.hamcrest.Matchers.matchesPattern("\\d+")))
                .andExpect(content().string(TooManyRequestsException.MESSAGE));
    }

    private org.springframework.test.web.servlet.ResultActions attemptLogin(String username, String password, String ip)
            throws Exception {
        assertTrue(ip != null && !ip.isBlank());
        return mockMvc.perform(post("/api/v1/auth/login")
                .with(request -> {
                    request.setRemoteAddr(ip);
                    return request;
                })
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"username\":\"" + username + "\",\"password\":\"" + password + "\"}"));
    }

    @Test
    void concurrencyFiftyThreadsForOneUsernameGivesExactlyFiveTo401Rest429() throws Exception {
        ensureUser("concurrent-user", "correct-pass");
        java.util.concurrent.CyclicBarrier barrier = new java.util.concurrent.CyclicBarrier(50);
        java.util.concurrent.atomic.AtomicInteger unauthorized = new java.util.concurrent.atomic.AtomicInteger();
        java.util.concurrent.atomic.AtomicInteger tooMany = new java.util.concurrent.atomic.AtomicInteger();
        java.util.concurrent.ExecutorService pool = java.util.concurrent.Executors.newFixedThreadPool(50);
        for (int i = 0; i < 50; i++) {
            final int idx = i;
            pool.submit(() -> {
                try {
                    barrier.await();
                    var res = attemptLogin("concurrent-user", "wrong-" + idx, "198.51.100.200").andReturn();
                    int status = res.getResponse().getStatus();
                    if (status == 401) unauthorized.incrementAndGet();
                    if (status == 429) tooMany.incrementAndGet();
                } catch (Exception e) {
                }
            });
        }
        pool.shutdown();
        pool.awaitTermination(120, java.util.concurrent.TimeUnit.SECONDS);
        assertEquals(5, unauthorized.get());
        assertEquals(45, tooMany.get());
    }

    private void ensureUser(String username, String password) {
        if (userRepository.findByUsername(username).isEmpty()) {
            userRepository.save(new AppUser(username, passwordEncoder.encode(password), "ADMIN", Instant.now()));
        }
    }
}
