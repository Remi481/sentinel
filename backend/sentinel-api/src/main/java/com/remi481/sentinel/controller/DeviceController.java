package com.remi481.sentinel.controller;

import com.remi481.sentinel.entity.Device;
import com.remi481.sentinel.service.DeviceService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.remi481.sentinel.dto.HeartbeatRequest;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/devices")
public class DeviceController {

    private final DeviceService deviceService;

    public DeviceController(DeviceService deviceService) {
        this.deviceService = deviceService;
    }

    @PostMapping("/register")
    public ResponseEntity<Device> registerDevice(@RequestBody Device device) {
        Device registeredDevice = deviceService.registerDevice(device);
        return ResponseEntity.ok(registeredDevice);
    }

    @PostMapping("/heartbeat")
    public ResponseEntity<Device> heartbeat(@RequestBody HeartbeatRequest request) {
        Device device = deviceService.processHeartbeat(request.uuid());
        return ResponseEntity.ok(device);
    }

    @GetMapping("/{uuid}")
    public ResponseEntity<Device> getDevice(@PathVariable UUID uuid) {
        Device device = deviceService.getDevice(uuid);
        return ResponseEntity.ok(device);
    }
}
