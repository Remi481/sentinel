package com.remi481.sentinel.service;

import com.remi481.sentinel.entity.Device;
import com.remi481.sentinel.entity.DeviceStatus;
import com.remi481.sentinel.repository.DeviceRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DeviceServiceTest {

    private static final long THRESHOLD_SECONDS = 90;

    @Mock
    private DeviceRepository deviceRepository;

    private DeviceService newService() {
        return new DeviceService(deviceRepository, THRESHOLD_SECONDS);
    }

    private Device device(UUID uuid, DeviceStatus status, Instant lastSeen) {
        Device device = new Device();
        device.setUuid(uuid);
        device.setStatus(status);
        device.setLastSeen(lastSeen);
        return device;
    }

    @Test
    void marksStaleOnlineDeviceOfflineAndKeepsLastSeen() {
        UUID uuid = UUID.randomUUID();
        Instant oldLastSeen = Instant.now().minusSeconds(600);
        Device stale = device(uuid, DeviceStatus.ONLINE, oldLastSeen);

        when(deviceRepository.findByStatusAndLastSeenBefore(eq(DeviceStatus.ONLINE), any(Instant.class)))
                .thenReturn(List.of(stale));
        when(deviceRepository.findByStatusAndLastSeenIsNull(DeviceStatus.ONLINE))
                .thenReturn(List.of());

        int changed = newService().markStaleDevicesOffline();

        assertEquals(1, changed);
        assertEquals(DeviceStatus.OFFLINE, stale.getStatus());
        assertEquals(oldLastSeen, stale.getLastSeen());
        verify(deviceRepository).saveAll(List.of(stale));
    }

    @Test
    void leavesRecentlySeenDeviceAlone() {
        when(deviceRepository.findByStatusAndLastSeenBefore(eq(DeviceStatus.ONLINE), any(Instant.class)))
                .thenReturn(List.of());
        when(deviceRepository.findByStatusAndLastSeenIsNull(DeviceStatus.ONLINE))
                .thenReturn(List.of());

        int changed = newService().markStaleDevicesOffline();

        assertEquals(0, changed);
        verify(deviceRepository, never()).saveAll(any());
    }

    @Test
    void marksOnlineDeviceWithNullLastSeenOffline() {
        UUID uuid = UUID.randomUUID();
        Device neverSeen = device(uuid, DeviceStatus.ONLINE, null);

        when(deviceRepository.findByStatusAndLastSeenBefore(eq(DeviceStatus.ONLINE), any(Instant.class)))
                .thenReturn(List.of());
        when(deviceRepository.findByStatusAndLastSeenIsNull(DeviceStatus.ONLINE))
                .thenReturn(List.of(neverSeen));

        int changed = newService().markStaleDevicesOffline();

        assertEquals(1, changed);
        assertEquals(DeviceStatus.OFFLINE, neverSeen.getStatus());
        assertNull(neverSeen.getLastSeen());
    }

    @Test
    void heartbeatOnOfflineDeviceMakesItOnlineAgain() {
        UUID uuid = UUID.randomUUID();
        Device offline = device(uuid, DeviceStatus.OFFLINE, Instant.now().minusSeconds(600));
        when(deviceRepository.findByUuid(uuid)).thenReturn(Optional.of(offline));
        when(deviceRepository.save(offline)).thenReturn(offline);

        Instant before = Instant.now();
        newService().processHeartbeat(uuid);

        assertEquals(DeviceStatus.ONLINE, offline.getStatus());
        assertNotNull(offline.getLastSeen());
        assertFalse(offline.getLastSeen().isBefore(before));
        verify(deviceRepository).save(offline);
    }

    @Test
    void returningDeviceRegistrationMakesItOnlineWithFreshLastSeen() {
        UUID uuid = UUID.randomUUID();
        Device existing = device(uuid, DeviceStatus.OFFLINE, Instant.now().minusSeconds(600));
        when(deviceRepository.findByUuid(uuid)).thenReturn(Optional.of(existing));
        when(deviceRepository.save(existing)).thenReturn(existing);

        Device incoming = device(uuid, DeviceStatus.OFFLINE, null);
        Instant before = Instant.now();
        newService().registerDevice(incoming);

        assertEquals(DeviceStatus.ONLINE, existing.getStatus());
        assertNotNull(existing.getLastSeen());
        assertFalse(existing.getLastSeen().isBefore(before));
        verify(deviceRepository).save(existing);
    }

    @Test
    void newDeviceRegistrationIsOnlineWithFreshLastSeen() {
        UUID uuid = UUID.randomUUID();
        when(deviceRepository.findByUuid(uuid)).thenReturn(Optional.empty());
        when(deviceRepository.save(any(Device.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Device incoming = device(uuid, DeviceStatus.UNKNOWN, null);
        Instant before = Instant.now();
        Device saved = newService().registerDevice(incoming);

        assertEquals(DeviceStatus.ONLINE, saved.getStatus());
        assertNotNull(saved.getLastSeen());
        assertFalse(saved.getLastSeen().isBefore(before));
    }
}
