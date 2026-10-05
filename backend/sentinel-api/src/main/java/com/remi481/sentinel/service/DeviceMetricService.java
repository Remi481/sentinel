package com.remi481.sentinel.service;

import com.remi481.sentinel.dto.MetricsRequest;
import com.remi481.sentinel.dto.MetricsResponse;
import com.remi481.sentinel.entity.Device;
import com.remi481.sentinel.entity.DeviceMetric;
import com.remi481.sentinel.repository.DeviceMetricRepository;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
public class DeviceMetricService {

    private final DeviceMetricRepository deviceMetricRepository;
    private final DeviceService deviceService;

    public DeviceMetricService(DeviceMetricRepository deviceMetricRepository,
                               DeviceService deviceService) {
        this.deviceMetricRepository = deviceMetricRepository;
        this.deviceService = deviceService;
    }

    public DeviceMetric recordMetrics(MetricsRequest request) {
        Device device = deviceService.getDevice(request.uuid());

        DeviceMetric metric = new DeviceMetric();
        metric.setDevice(device);
        metric.setRecordedAt(Instant.now());
        metric.setCpuUsagePercent(request.cpuUsagePercent());
        metric.setMemoryUsedBytes(request.memoryUsedBytes());
        metric.setMemoryTotalBytes(request.memoryTotalBytes());
        metric.setDiskUsedBytes(request.diskUsedBytes());
        metric.setDiskTotalBytes(request.diskTotalBytes());

        return deviceMetricRepository.save(metric);
    }

    public List<MetricsResponse> getMetricsForDevice(UUID uuid) {
        deviceService.getDevice(uuid);

        return deviceMetricRepository.findTop100ByDeviceUuidOrderByRecordedAtDesc(uuid)
                .stream()
                .map(metric -> new MetricsResponse(
                        metric.getRecordedAt(),
                        metric.getCpuUsagePercent(),
                        metric.getMemoryUsedBytes(),
                        metric.getMemoryTotalBytes(),
                        metric.getDiskUsedBytes(),
                        metric.getDiskTotalBytes()))
                .toList();
    }
}