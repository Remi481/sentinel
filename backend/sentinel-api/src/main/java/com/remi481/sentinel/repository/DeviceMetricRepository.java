package com.remi481.sentinel.repository;

import com.remi481.sentinel.entity.DeviceMetric;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface DeviceMetricRepository extends JpaRepository<DeviceMetric, Long> {

    List<DeviceMetric> findTop100ByDeviceUuidOrderByRecordedAtDesc(UUID uuid);
}