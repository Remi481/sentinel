package com.remi481.sentinel.controller;

import com.remi481.sentinel.entity.Device;
import com.remi481.sentinel.service.DeviceService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

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
}
