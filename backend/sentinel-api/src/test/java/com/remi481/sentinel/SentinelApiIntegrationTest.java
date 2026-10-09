package com.remi481.sentinel;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class SentinelApiIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    private static final UUID DEVICE_UUID = UUID.fromString("550e8400-e29b-41d4-a716-446655440000");
    private static final String UNKNOWN_UUID = "11111111-2222-3333-4444-555555555555";

    @Test
    void deviceLifecycleEndToEnd() throws Exception {
        mockMvc.perform(post("/api/v1/devices/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(deviceJson(DEVICE_UUID)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.uuid").value(DEVICE_UUID.toString()))
                .andExpect(jsonPath("$.hostname").value("integration-host"))
                .andExpect(jsonPath("$.status").value("ONLINE"))
                .andExpect(jsonPath("$.lastSeen").isNotEmpty());

        mockMvc.perform(get("/api/v1/devices/{uuid}", DEVICE_UUID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.hostname").value("integration-host"));

        mockMvc.perform(get("/api/v1/devices"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].uuid").value(DEVICE_UUID.toString()));

        mockMvc.perform(post("/api/v1/devices/heartbeat")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"uuid\": \"" + DEVICE_UUID + "\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ONLINE"));

        mockMvc.perform(post("/api/v1/metrics")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(deviceMetrics(DEVICE_UUID, 100)))
                .andExpect(status().isCreated());
        mockMvc.perform(post("/api/v1/metrics")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(deviceMetrics(DEVICE_UUID, 200)))
                .andExpect(status().isCreated());
        mockMvc.perform(post("/api/v1/metrics")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(deviceMetrics(DEVICE_UUID, 300)))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/v1/devices/{uuid}/metrics", DEVICE_UUID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(3))
                .andExpect(jsonPath("$[0].uptimeSeconds").value(300))
                .andExpect(jsonPath("$[1].uptimeSeconds").value(200))
                .andExpect(jsonPath("$[2].uptimeSeconds").value(100))
                .andExpect(jsonPath("$[0].networkReceivedBytesPerSecond").value(150))
                .andExpect(jsonPath("$[0].networkSentBytesPerSecond").value(250))
                .andExpect(jsonPath("$[0].recordedAt").isNotEmpty());
    }

    @Test
    void unknownUuidReturnsNotFoundWithMessage() throws Exception {
        String expectedBody = "Device not found: " + UNKNOWN_UUID;

        mockMvc.perform(get("/api/v1/devices/{uuid}", UNKNOWN_UUID))
                .andExpect(status().isNotFound())
                .andExpect(content().string(expectedBody));

        mockMvc.perform(get("/api/v1/devices/{uuid}/metrics", UNKNOWN_UUID))
                .andExpect(status().isNotFound())
                .andExpect(content().string(expectedBody));

        mockMvc.perform(post("/api/v1/devices/heartbeat")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"uuid\": \"" + UNKNOWN_UUID + "\"}"))
                .andExpect(status().isNotFound())
                .andExpect(content().string(expectedBody));

        mockMvc.perform(post("/api/v1/metrics")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(deviceMetrics(UUID.fromString(UNKNOWN_UUID), 100)))
                .andExpect(status().isNotFound())
                .andExpect(content().string(expectedBody));
    }

    private String deviceJson(UUID uuid) {
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

    private String deviceMetrics(UUID uuid, long uptimeSeconds) {
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