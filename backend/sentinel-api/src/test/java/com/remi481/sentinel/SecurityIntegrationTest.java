package com.remi481.sentinel;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import com.remi481.sentinel.entity.Device;
import com.remi481.sentinel.repository.DeviceRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class SecurityIntegrationTest {

    private static final String ENROLLMENT_KEY = "test-enrollment-key-1234567890abcdef";
    private static final String WRONG_KEY = "definitely-not-the-right-key-000000";
    private static final String ADMIN_USERNAME = "test-admin";
    private static final String ADMIN_PASSWORD = "test-admin-password-1234";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private DeviceRepository deviceRepository;

    @Test
    void healthEndpointIsPublic() throws Exception {
        mockMvc.perform(get("/api/v1/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"));
    }

    @Test
    void loginReturnsTokenWithExpiry() throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\": \"" + ADMIN_USERNAME + "\", \"password\": \"" + ADMIN_PASSWORD + "\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.expiresInSeconds").value(3600))
                .andReturn();

        JsonNode body = objectMapper.readTree(result.getResponse().getContentAsString());
        assertFalse(body.path("token").asText().isBlank());

        mockMvc.perform(get("/api/v1/devices")
                        .header("Authorization", "Bearer " + body.path("token").asText()))
                .andExpect(status().isOk());
    }

    @Test
    void loginWithWrongPasswordReturns401() throws Exception {
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\": \"" + ADMIN_USERNAME + "\", \"password\": \"wrong-password-1234\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(content().string("Invalid username or password"));
    }

    @Test
    void loginWithUnknownUserReturnsSame401Body() throws Exception {
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\": \"no-such-user\", \"password\": \"wrong-password-1234\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(content().string("Invalid username or password"));
    }

    @Test
    void loginWithMissingFieldsReturns400() throws Exception {
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void dashboardEndpointsRequireJwt() throws Exception {
        String uuid = UUID.randomUUID().toString();

        mockMvc.perform(get("/api/v1/devices"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/v1/devices/{uuid}", uuid))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/v1/devices/{uuid}/metrics", uuid))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void registerRequiresEnrollmentKey() throws Exception {
        String uuid = UUID.randomUUID().toString();

        mockMvc.perform(post("/api/v1/devices/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(deviceJson(uuid)))
                .andExpect(status().isUnauthorized())
                .andExpect(content().string("Invalid or missing credentials"));

        mockMvc.perform(post("/api/v1/devices/register")
                        .header("X-Enrollment-Key", WRONG_KEY)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(deviceJson(uuid)))
                .andExpect(status().isUnauthorized())
                .andExpect(content().string("Invalid or missing credentials"));
    }

    @Test
    void successfulEnrolmentReturnsTokenAndNeverExposesTokenHash() throws Exception {
        String uuid = UUID.randomUUID().toString();

        MvcResult enrolResult = mockMvc.perform(post("/api/v1/devices/register")
                        .header("X-Enrollment-Key", ENROLLMENT_KEY)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(deviceJson(uuid)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.device.uuid").value(uuid))
                .andExpect(jsonPath("$.deviceToken").isNotEmpty())
                .andExpect(jsonPath("$.deviceToken").value(not(nullValue())))
                .andExpect(jsonPath("$.device.tokenHash").doesNotExist())
                .andExpect(content().string(not(org.hamcrest.Matchers.containsString("tokenHash"))))
                .andExpect(content().string(not(org.hamcrest.Matchers.containsString("token_hash"))))
                .andReturn();

        String token = objectMapper.readTree(enrolResult.getResponse().getContentAsString())
                .path("deviceToken").asText();
        Device stored = deviceRepository.findByUuid(UUID.fromString(uuid)).orElseThrow();
        String storedHash = stored.getTokenHash();
        assertNotNull(storedHash);
        assertEquals(64, storedHash.length());
        assertTrue(storedHash.matches("[0-9a-f]{64}"));
        assertNotEquals(token, storedHash);

        String jwt = loginToken();
        mockMvc.perform(get("/api/v1/devices/{uuid}", uuid)
                        .header("Authorization", "Bearer " + jwt))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tokenHash").doesNotExist())
                .andExpect(result -> {
                    String body = result.getResponse().getContentAsString();
                    assertFalse(body.contains("tokenHash"));
                    assertFalse(body.contains("token_hash"));
                    assertFalse(body.contains(token));
                });

        mockMvc.perform(get("/api/v1/devices")
                        .header("Authorization", "Bearer " + jwt))
                .andExpect(status().isOk())
                .andExpect(result -> {
                    String body = result.getResponse().getContentAsString();
                    assertFalse(body.contains("tokenHash"));
                    assertFalse(body.contains("token_hash"));
                    assertFalse(body.contains(token));
                });
    }

    @Test
    void reRegisterWithEnrollmentKeyOnlyRejectedAfterEnrolment() throws Exception {
        String uuid = UUID.randomUUID().toString();
        enrol(uuid);

        mockMvc.perform(post("/api/v1/devices/register")
                        .header("X-Enrollment-Key", ENROLLMENT_KEY)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(deviceJson(uuid)))
                .andExpect(status().isUnauthorized())
                .andExpect(content().string("Invalid or missing credentials"));
    }

    @Test
    void reRegisterWithOwnTokenSucceedsWithNullDeviceToken() throws Exception {
        String uuid = UUID.randomUUID().toString();
        String token = enrol(uuid).path("deviceToken").asText();

        mockMvc.perform(post("/api/v1/devices/register")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(deviceJson(uuid)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.device.uuid").value(uuid))
                .andExpect(jsonPath("$.deviceToken").value(nullValue()));
    }

    @Test
    void heartbeatRequiresMatchingDeviceToken() throws Exception {
        String firstUuid = UUID.randomUUID().toString();
        String secondUuid = UUID.randomUUID().toString();
        String firstToken = enrol(firstUuid).path("deviceToken").asText();
        String secondToken = enrol(secondUuid).path("deviceToken").asText();

        mockMvc.perform(post("/api/v1/devices/heartbeat")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"uuid\": \"" + firstUuid + "\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(content().string("Invalid or missing credentials"));

        mockMvc.perform(post("/api/v1/devices/heartbeat")
                        .header("Authorization", "Bearer " + secondToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"uuid\": \"" + firstUuid + "\"}"))
                .andExpect(status().isForbidden())
                .andExpect(content().string("Device token does not match the requested device"));

        mockMvc.perform(post("/api/v1/devices/heartbeat")
                        .header("Authorization", "Bearer " + firstToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"uuid\": \"" + firstUuid + "\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ONLINE"));
    }

    @Test
    void metricsRequireMatchingDeviceToken() throws Exception {
        String firstUuid = UUID.randomUUID().toString();
        String secondUuid = UUID.randomUUID().toString();
        String firstToken = enrol(firstUuid).path("deviceToken").asText();
        String secondToken = enrol(secondUuid).path("deviceToken").asText();

        mockMvc.perform(post("/api/v1/metrics")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(deviceMetrics(firstUuid)))
                .andExpect(status().isUnauthorized())
                .andExpect(content().string("Invalid or missing credentials"));

        mockMvc.perform(post("/api/v1/metrics")
                        .header("Authorization", "Bearer " + secondToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(deviceMetrics(firstUuid)))
                .andExpect(status().isForbidden());

        mockMvc.perform(post("/api/v1/metrics")
                        .header("Authorization", "Bearer " + firstToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(deviceMetrics(firstUuid)))
                .andExpect(status().isCreated());
    }

    @Test
    void revokedDeviceTokenRejectedThenDeviceCanReEnrol() throws Exception {
        String uuid = UUID.randomUUID().toString();
        String oldToken = enrol(uuid).path("deviceToken").asText();
        String jwt = loginToken();

        mockMvc.perform(post("/api/v1/devices/{uuid}/revoke-token", uuid)
                        .header("Authorization", "Bearer " + jwt))
                .andExpect(status().isNoContent());

        mockMvc.perform(post("/api/v1/devices/heartbeat")
                        .header("Authorization", "Bearer " + oldToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"uuid\": \"" + uuid + "\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(content().string("Invalid or missing credentials"));

        JsonNode reEnrolment = enrol(uuid);
        String newToken = reEnrolment.path("deviceToken").asText();
        assertFalse(newToken.isBlank());
        assertFalse(newToken.equals(oldToken));

        mockMvc.perform(post("/api/v1/devices/heartbeat")
                        .header("Authorization", "Bearer " + newToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"uuid\": \"" + uuid + "\"}"))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/v1/devices/register")
                        .header("X-Enrollment-Key", ENROLLMENT_KEY)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(deviceJson(uuid)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void revokeRequiresAdminJwt() throws Exception {
        String uuid = UUID.randomUUID().toString();
        String deviceToken = enrol(uuid).path("deviceToken").asText();

        mockMvc.perform(post("/api/v1/devices/{uuid}/revoke-token", uuid))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(post("/api/v1/devices/{uuid}/revoke-token", uuid)
                        .header("Authorization", "Bearer " + deviceToken))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void revokeUnknownDeviceReturns404() throws Exception {
        String uuid = UUID.randomUUID().toString();
        String jwt = loginToken();

        mockMvc.perform(post("/api/v1/devices/{uuid}/revoke-token", uuid)
                        .header("Authorization", "Bearer " + jwt))
                .andExpect(status().isNotFound())
                .andExpect(content().string("Device not found: " + uuid));
    }

    private String loginToken() throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\": \"" + ADMIN_USERNAME + "\", \"password\": \"" + ADMIN_PASSWORD + "\"}"))
                .andExpect(status().isOk())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString()).path("token").asText();
    }

    private JsonNode enrol(String uuid) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/devices/register")
                        .header("X-Enrollment-Key", ENROLLMENT_KEY)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(deviceJson(uuid)))
                .andExpect(status().isOk())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString());
    }

    private String deviceJson(String uuid) {
        return """
                {
                  "uuid": "%s",
                  "hostname": "security-test-host",
                  "operatingSystem": "Linux",
                  "architecture": "x86_64",
                  "ipAddress": "10.0.0.9",
                  "agentVersion": "0.1.0",
                  "status": "ONLINE",
                  "lastSeen": null
                }
                """.formatted(uuid);
    }

    private String deviceMetrics(String uuid) {
        return """
                {
                  "uuid": "%s",
                  "cpuUsagePercent": 42.0,
                  "memoryUsedBytes": 1000,
                  "memoryTotalBytes": 2000,
                  "diskUsedBytes": 3000,
                  "diskTotalBytes": 4000,
                  "uptimeSeconds": 60,
                  "networkReceivedBytesPerSecond": 150,
                  "networkSentBytesPerSecond": 250
                }
                """.formatted(uuid);
    }
}