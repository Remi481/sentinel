package com.remi481.sentinel.repository;

import com.remi481.sentinel.entity.Device;
import com.remi481.sentinel.entity.DeviceStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface DeviceRepository extends JpaRepository<Device, UUID> {
    Optional<Device> findByUuid(UUID uuid);

    List<Device> findByStatusAndLastSeenBefore(DeviceStatus status, Instant cutoff);

    List<Device> findByStatusAndLastSeenIsNull(DeviceStatus status);
}
