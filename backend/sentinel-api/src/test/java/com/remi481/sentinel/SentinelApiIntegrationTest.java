package com.remi481.sentinel;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.UUID;

import static org.hamcrest.Matchers.hasItem;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class SentinelApiIntegrationTest {

    private static final String ENROLLMENT_KEY = "test-enrollment-key-1234567890abcdef";
    private static final String ADMIN_USERNAME = "test-admin";
    private static final String ADMIN_PASSWORD = "test-admin-password-1234";
    private static final String DEVICE_UUID = "550e8400-e29b-41d4-a716-446655440000";
    private static final String UNKNOWN_UUID = "11111111-2222-3333-4444-555555555555";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void deviceLifecycleEndToEnd() throws Exception {
        String jwt = loginToken();
        JsonNode enrolment = enrol(DEVICE_UUID);
        String deviceToken = enrolment.path("deviceToken").asText();

        mockMvc.perform(get("/api/v1/devices/{uuid}", DEVICE_UUID)
                        .header("Authorization", "Bearer " + jwt))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.hostname").value("integration-host"));

        mockMvc.perform(get("/api/v1/devices")
                        .header("Authorization", "Bearer " + jwt))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[*].uuid", hasItem(DEVICE_UUID)));

        mockMvc.perform(post("/api/v1/devices/heartbeat")
                        .header("Authorization", "Bearer " + deviceToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"uuid\": \"" + DEVICE_UUID + "\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ONLINE"));

        mockMvc.perform(post("/api/v1/metrics")
                        .header("Authorization", "Bearer " + deviceToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(deviceMetrics(DEVICE_UUID, 100)))
                .andExpect(status().isCreated());
        mockMvc.perform(post("/api/v1/metrics")
                        .header("Authorization", "Bearer " + deviceToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(deviceMetrics(DEVICE_UUID, 200)))
                .andExpect(status().isCreated());
        mockMvc.perform(post("/api/v1/metrics")
                        .header("Authorization", "Bearer " + deviceToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(deviceMetrics(DEVICE_UUID, 300)))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/v1/devices/{uuid}/metrics", DEVICE_UUID)
                        .header("Authorization", "Bearer " + jwt))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(3))
                .andExpect(jsonPath("$[0].uptimeSeconds").value(300))
                .andExpect(jsonPath("$[1].uptimeSeconds").value(200))
                .andExpect(jsonPath("$[2].uptimeSeconds").value(100))
                .andExpect(jsonPath("$[0].recordedAt").isNotEmpty());
    }

    @Test
    void unknownUuidReturnsNotFoundWithMessage() throws Exception {
        String jwt = loginToken();
        String expectedBody = "Device not found: " + UNKNOWN_UUID;

        mockMvc.perform(get("/api/v1/devices/{uuid}", UNKNOWN_UUID)
                        .header("Authorization", "Bearer " + jwt))
                .andExpect(status().isNotFound())
                .andExpect(content().string(expectedBody));

        mockMvc.perform(get("/api/v1/devices/{uuid}/metrics", UNKNOWN_UUID)
                        .header("Authorization", "Bearer " + jwt))
                .andExpect(status().isNotFound())
                .andExpect(content().string(expectedBody));

        String deviceToken = enrol("22222222-3333-4444-5555-666666666666").path("deviceToken").asText();

        mockMvc.perform(post("/api/v1/devices/heartbeat")
                        .header("Authorization", "Bearer " + deviceToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"uuid\": \"" + UNKNOWN_UUID + "\"}"))
                .andExpect(status().isForbidden())
                .andExpect(content().string("Device token does not match the requested device"));

        mockMvc.perform(post("/api/v1/metrics")
                        .header("Authorization", "Bearer " + deviceToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(deviceMetrics(UNKNOWN_UUID, 100)))
                .andExpect(status().isForbidden())
                .andExpect(content().string("Device token does not match the requested device"));
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
                  "hostname": "integration-host",
                  "operatingSystem": "Linux",
                  "architecture": "x86_64",
                  "ipAddress": "10.0.0.5",
                  "agentVersion": "0.1.0",
                  "status": "ONLINE",
                  "lastSeen": null
                }
                """.formatted(uuid);
    }

    private String deviceMetrics(String uuid, long uptimeSeconds) {
        return """
                {
                  "uuid": "%s",
                  "cpuUsagePercent": 12.3,
                  "memoryUsedBytes": 1000,
                  "memoryTotalBytes": 2000,
                  "diskUsedBytes": 3000,
                  "diskTotalBytes": 4000,
                  "uptimeSeconds": %d,
                  "networkReceivedBytesPerSecond": 150,
                  "networkSentBytesPerSecond": 250
                }
                """.formatted(uuid, uptimeSeconds);
    }
}