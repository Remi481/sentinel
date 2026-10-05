package com.remi481.sentinel.controller;

import com.remi481.sentinel.dto.MetricsRequest;
import com.remi481.sentinel.dto.MetricsResponse;
import com.remi481.sentinel.service.DeviceMetricService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1")
public class DeviceMetricController {

    private final DeviceMetricService deviceMetricService;

    public DeviceMetricController(DeviceMetricService deviceMetricService) {
        this.deviceMetricService = deviceMetricService;
    }

    @PostMapping("/metrics")
    public ResponseEntity<Void> recordMetrics(@RequestBody MetricsRequest request) {
        deviceMetricService.recordMetrics(request);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @GetMapping("/devices/{uuid}/metrics")
    public ResponseEntity<List<MetricsResponse>> getMetrics(@PathVariable UUID uuid) {
        return ResponseEntity.ok(deviceMetricService.getMetricsForDevice(uuid));
    }
}