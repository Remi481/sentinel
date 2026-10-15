package com.remi481.sentinel.controller;

import com.remi481.sentinel.dto.MetricsRequest;
import com.remi481.sentinel.dto.MetricsResponse;
import com.remi481.sentinel.entity.Device;
import com.remi481.sentinel.exception.ForbiddenException;
import com.remi481.sentinel.security.AgentAuthService;
import com.remi481.sentinel.service.DeviceMetricService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1")
public class DeviceMetricController {

    private final DeviceMetricService deviceMetricService;
    private final AgentAuthService agentAuthService;

    public DeviceMetricController(DeviceMetricService deviceMetricService,
                                  AgentAuthService agentAuthService) {
        this.deviceMetricService = deviceMetricService;
        this.agentAuthService = agentAuthService;
    }

    @PostMapping("/metrics")
    public ResponseEntity<Void> recordMetrics(@Valid @RequestBody MetricsRequest request,
                                              @RequestHeader(value = "Authorization", required = false) String authorization) {
        Device authenticated = agentAuthService.authenticateDeviceToken(authorization);
        if (!authenticated.getUuid().equals(request.uuid())) {
            throw new ForbiddenException();
        }
        deviceMetricService.recordMetrics(request);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @GetMapping("/devices/{uuid}/metrics")
    public ResponseEntity<List<MetricsResponse>> getMetrics(@PathVariable UUID uuid) {
        return ResponseEntity.ok(deviceMetricService.getMetricsForDevice(uuid));
    }
}