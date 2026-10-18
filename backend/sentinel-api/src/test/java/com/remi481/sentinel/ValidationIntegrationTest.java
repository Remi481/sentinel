package com.remi481.sentinel;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class ValidationIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void registerWithoutUuidReturnsBadRequestJson() throws Exception {
        mockMvc.perform(post("/api/v1/devices/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"hostname\":\"host\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.error").isNotEmpty());
    }

    @Test
    void registerWithoutHostnameReturnsBadRequestJson() throws Exception {
        mockMvc.perform(post("/api/v1/devices/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"uuid\":\"" + UUID.randomUUID() + "\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").isNotEmpty());
    }

    @Test
    void registerWithLongHostnameReturnsBadRequest() throws Exception {
        mockMvc.perform(post("/api/v1/devices/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"uuid\":\"" + UUID.randomUUID() + "\",\"hostname\":\"" + "h".repeat(256) + "\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").isNotEmpty());
    }

    @Test
    void registerWithLongOperatingSystemReturnsBadRequest() throws Exception {
        mockMvc.perform(post("/api/v1/devices/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"uuid\":\"" + UUID.randomUUID() + "\",\"hostname\":\"host\","
                                + "\"operatingSystem\":\"" + "o".repeat(256) + "\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").isNotEmpty());
    }

    @Test
    void registerWithMalformedJsonReturnsBadRequestJson() throws Exception {
        mockMvc.perform(post("/api/v1/devices/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{not json"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").isNotEmpty());
    }

    @Test
    void registerWithoutEnrollmentKeyStillReturnsUnauthorizedForAValidBody() throws Exception {
        mockMvc.perform(post("/api/v1/devices/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"uuid\":\"" + UUID.randomUUID() + "\",\"hostname\":\"host\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void heartbeatWithoutUuidReturnsBadRequest() throws Exception {
        mockMvc.perform(post("/api/v1/devices/heartbeat")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").isNotEmpty());
    }

    @Test
    void heartbeatWithValidShapeButNoTokenReturnsUnauthorized() throws Exception {
        mockMvc.perform(post("/api/v1/devices/heartbeat")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"uuid\":\"" + UUID.randomUUID() + "\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void metricsWithoutUuidReturnsBadRequest() throws Exception {
        mockMvc.perform(post("/api/v1/metrics")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").isNotEmpty());
    }

    @Test
    void metricsWithCpuAboveOneHundredReturnsBadRequest() throws Exception {
        mockMvc.perform(post("/api/v1/metrics")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(metricsJson("\"cpuUsagePercent\": 100.1")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").isNotEmpty());
    }

    @Test
    void metricsWithNegativeCpuReturnsBadRequest() throws Exception {
        mockMvc.perform(post("/api/v1/metrics")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(metricsJson("\"cpuUsagePercent\": -0.1")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").isNotEmpty());
    }

    @Test
    void metricsWithNegativeBytesReturnsBadRequest() throws Exception {
        mockMvc.perform(post("/api/v1/metrics")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(metricsJson("\"memoryUsedBytes\": -1")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").isNotEmpty());
    }

    @Test
    void metricsWithNegativeUptimeReturnsBadRequest() throws Exception {
        mockMvc.perform(post("/api/v1/metrics")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(metricsJson("\"uptimeSeconds\": -1")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").isNotEmpty());
    }

    @Test
    void loginWithTooLongUsernameReturnsBadRequest() throws Exception {
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"" + "u".repeat(201) + "\",\"password\":\"whatever-1234\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").isNotEmpty());
    }

    private String metricsJson(String extraField) {
        return "{\"uuid\":\"" + UUID.randomUUID() + "\"," + extraField + "}";
    }
}
