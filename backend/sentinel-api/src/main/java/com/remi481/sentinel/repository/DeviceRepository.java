package com.remi481.sentinel.repository;

import com.remi481.sentinel.entity.Device;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface DeviceRepository extends JpaRepository<Device, UUID> {

    Optional<Device> findByUuid(UUID uuid);

}