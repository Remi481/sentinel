package com.remi481.sentinel.repository;

import com.remi481.sentinel.entity.DeviceMetric;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DeviceMetricRepository extends JpaRepository<DeviceMetric, Long> {
}
