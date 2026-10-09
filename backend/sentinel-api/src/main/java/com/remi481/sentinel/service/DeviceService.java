package com.remi481.sentinel.service;

import com.remi481.sentinel.entity.Device;
import com.remi481.sentinel.exception.DeviceNotFoundException;
import com.remi481.sentinel.repository.DeviceRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import com.remi481.sentinel.entity.DeviceStatus;

@Service
public class DeviceService {

    private final DeviceRepository deviceRepository;
    private final long offlineThresholdSeconds;

    public DeviceService(DeviceRepository deviceRepository,
                         @Value("${sentinel.offline.threshold-seconds}") long offlineThresholdSeconds) {
        this.deviceRepository = deviceRepository;
        this.offlineThresholdSeconds = offlineThresholdSeconds;
    }

    public Device registerDevice(Device device) {

        return deviceRepository.findByUuid(device.getUuid())
                .map(existingDevice -> {
                    existingDevice.setHostname(device.getHostname());
                    existingDevice.setOperatingSystem(device.getOperatingSystem());
                    existingDevice.setArchitecture(device.getArchitecture());
                    existingDevice.setIpAddress(device.getIpAddress());
                    existingDevice.setAgentVersion(device.getAgentVersion());
                    existingDevice.setStatus(DeviceStatus.ONLINE);
                    existingDevice.setLastSeen(Instant.now());

                    return deviceRepository.save(existingDevice);
                })
                .orElseGet(() -> {
                    device.setStatus(DeviceStatus.ONLINE);
                    device.setLastSeen(Instant.now());

                    return deviceRepository.save(device);
                });


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

    @Transactional
    public int markStaleDevicesOffline() {
        Instant cutoff = Instant.now().minusSeconds(offlineThresholdSeconds);

        List<Device> staleDevices = new ArrayList<>();
        staleDevices.addAll(deviceRepository.findByStatusAndLastSeenBefore(DeviceStatus.ONLINE, cutoff));
        staleDevices.addAll(deviceRepository.findByStatusAndLastSeenIsNull(DeviceStatus.ONLINE));

        if (staleDevices.isEmpty()) {
            return 0;
        }

        staleDevices.forEach(device -> device.setStatus(DeviceStatus.OFFLINE));
        deviceRepository.saveAll(staleDevices);

        return staleDevices.size();
    }

    public Device getDevice(UUID uuid) {
        return deviceRepository.findByUuid(uuid)
                .orElseThrow(() -> new DeviceNotFoundException(uuid));
    }

    public List<Device> getAllDevices() {
        return deviceRepository.findAll();
    }


}
