package com.remi481.sentinel.controller;

import com.remi481.sentinel.dto.DeviceRegistration;
import com.remi481.sentinel.dto.HeartbeatRequest;
import com.remi481.sentinel.entity.Device;
import com.remi481.sentinel.exception.ForbiddenException;
import com.remi481.sentinel.security.AgentAuthService;
import com.remi481.sentinel.service.DeviceAuthService;
import com.remi481.sentinel.service.DeviceService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/devices")
public class DeviceController {

    private final DeviceService deviceService;
    private final DeviceAuthService deviceAuthService;
    private final AgentAuthService agentAuthService;

    public DeviceController(DeviceService deviceService,
                            DeviceAuthService deviceAuthService,
                            AgentAuthService agentAuthService) {
        this.deviceService = deviceService;
        this.deviceAuthService = deviceAuthService;
        this.agentAuthService = agentAuthService;
    }

    @PostMapping("/register")
    public DeviceRegistration registerDevice(@RequestBody Device device,
                                             @RequestHeader(value = "X-Enrollment-Key", required = false) String enrollmentKey,
                                             @RequestHeader(value = "Authorization", required = false) String authorization) {
        boolean alreadyEnrolled = deviceAuthService.findByUuid(device.getUuid())
                .map(existing -> existing.getTokenHash() != null)
                .orElse(false);

        if (alreadyEnrolled) {
            Device authenticated = agentAuthService.authenticateDeviceToken(authorization);
            requireOwnDevice(authenticated, device.getUuid());
            return new DeviceRegistration(deviceService.registerDevice(device), null);
        }

        agentAuthService.requireEnrollmentKey(enrollmentKey);
        return deviceAuthService.enrolNewDevice(device);
    }

    @PostMapping("/heartbeat")
    public ResponseEntity<Device> heartbeat(@RequestBody HeartbeatRequest request,
                                            @RequestHeader(value = "Authorization", required = false) String authorization) {
        Device authenticated = agentAuthService.authenticateDeviceToken(authorization);
        requireOwnDevice(authenticated, request.uuid());
        Device device = deviceService.processHeartbeat(request.uuid());
        return ResponseEntity.ok(device);
    }

    @GetMapping("/{uuid}")
    public ResponseEntity<Device> getDevice(@PathVariable UUID uuid) {
        Device device = deviceService.getDevice(uuid);
        return ResponseEntity.ok(device);
    }

    @GetMapping
    public ResponseEntity<List<Device>> getAllDevices() {
        return ResponseEntity.ok(deviceService.getAllDevices());
    }

    @PostMapping("/{uuid}/revoke-token")
    public ResponseEntity<Void> revokeDeviceToken(@PathVariable UUID uuid) {
        deviceAuthService.revokeDeviceToken(uuid);
        return ResponseEntity.noContent().build();
    }

    private void requireOwnDevice(Device authenticated, UUID requestedUuid) {
        if (!authenticated.getUuid().equals(requestedUuid)) {
            throw new ForbiddenException();
        }
    }
}