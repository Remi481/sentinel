package com.remi481.sentinel.controller;

import com.remi481.sentinel.dto.MetricsRequest;
import com.remi481.sentinel.service.DeviceMetricService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/metrics")
public class DeviceMetricController {

    private final DeviceMetricService deviceMetricService;

    public DeviceMetricController(DeviceMetricService deviceMetricService) {
        this.deviceMetricService = deviceMetricService;
    }

    @PostMapping
    public ResponseEntity<Void> recordMetrics(@RequestBody MetricsRequest request) {
        deviceMetricService.recordMetrics(request);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }
}
