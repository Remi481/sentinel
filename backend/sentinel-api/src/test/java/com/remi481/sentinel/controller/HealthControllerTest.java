package com.remi481.sentinel.controller;

import com.remi481.sentinel.service.DatabaseHealthService;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class HealthControllerTest {

    private final DatabaseHealthService databaseHealthService = mock(DatabaseHealthService.class);
    private final HealthController controller = new HealthController(databaseHealthService);

    @Test
    void returnsOkWhenTheDatabaseAnswers() {
        when(databaseHealthService.isDatabaseAvailable()).thenReturn(true);

        ResponseEntity<Map<String, String>> response = controller.health();

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(Map.of("status", "UP"), response.getBody());
    }

    @Test
    void returnsServiceUnavailableWhenTheDatabaseFails() {
        when(databaseHealthService.isDatabaseAvailable()).thenReturn(false);

        ResponseEntity<Map<String, String>> response = controller.health();

        assertEquals(HttpStatus.SERVICE_UNAVAILABLE, response.getStatusCode());
        assertEquals(Map.of("status", "DOWN"), response.getBody());
    }
}
