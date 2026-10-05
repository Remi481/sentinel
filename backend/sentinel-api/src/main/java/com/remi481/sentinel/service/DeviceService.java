package com.remi481.sentinel.service;

import com.remi481.sentinel.entity.Device;
import com.remi481.sentinel.exception.DeviceNotFoundException;
import com.remi481.sentinel.repository.DeviceRepository;
import org.springframework.stereotype.Service;
import java.time.Instant;
import java.util.UUID;
import com.remi481.sentinel.entity.DeviceStatus;

@Service
public class DeviceService {

    private final DeviceRepository deviceRepository;

    public DeviceService(DeviceRepository deviceRepository) {
        this.deviceRepository = deviceRepository;
    }

    public Device registerDevice(Device device) {

        return deviceRepository.findByUuid(device.getUuid())
                .map(existingDevice -> {
                    existingDevice.setHostname(device.getHostname());
                    existingDevice.setOperatingSystem(device.getOperatingSystem());
                    existingDevice.setArchitecture(device.getArchitecture());
                    existingDevice.setIpAddress(device.getIpAddress());
                    existingDevice.setAgentVersion(device.getAgentVersion());
                    existingDevice.setStatus(device.getStatus());
                    existingDevice.setLastSeen(device.getLastSeen());

                    return deviceRepository.save(existingDevice);
                })
                .orElseGet(() -> deviceRepository.save(device));


    }

    public Device processHeartbeat(UUID uuid) {
        return deviceRepository.findByUuid(uuid)
                .map(device -> {
                    device.setStatus(DeviceStatus.ONLINE);
                    device.setLastSeen(Instant.now());

                    return deviceRepository.save(device);
                })
                .orElseThrow(() -> new DeviceNotFoundException(uuid));
    }

    public Device getDevice(UUID uuid) {
        return deviceRepository.findByUuid(uuid)
                .orElseThrow(() -> new DeviceNotFoundException(uuid));
    }


}
