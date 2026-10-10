package com.remi481.sentinel.service;

import com.remi481.sentinel.dto.DeviceRegistration;
import com.remi481.sentinel.entity.Device;
import com.remi481.sentinel.entity.DeviceStatus;
import com.remi481.sentinel.exception.DeviceNotFoundException;
import com.remi481.sentinel.repository.DeviceRepository;
import com.remi481.sentinel.security.DeviceTokenService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Service
public class DeviceAuthService {

    private final DeviceRepository deviceRepository;
    private final DeviceService deviceService;
    private final DeviceTokenService deviceTokenService;

    public DeviceAuthService(DeviceRepository deviceRepository,
                             DeviceService deviceService,
                             DeviceTokenService deviceTokenService) {
        this.deviceRepository = deviceRepository;
        this.deviceService = deviceService;
        this.deviceTokenService = deviceTokenService;
    }

    public Optional<Device> findByUuid(UUID uuid) {
        return deviceRepository.findByUuid(uuid);
    }

    public DeviceRegistration enrolNewDevice(Device incoming) {
        String deviceToken = deviceTokenService.generateDeviceToken();
        Device device = deviceService.registerDevice(incoming);
        device.setTokenHash(deviceTokenService.hash(deviceToken));
        device.setStatus(DeviceStatus.ONLINE);
        device.setLastSeen(Instant.now());
        return new DeviceRegistration(deviceRepository.save(device), deviceToken);
    }

    @Transactional
    public void revokeDeviceToken(UUID uuid) {
        deviceRepository.findByUuid(uuid).ifPresentOrElse(
                device -> {
                    device.setTokenHash(null);
                    deviceRepository.save(device);
                },
                () -> {
                    throw new DeviceNotFoundException(uuid);
                });
    }
}