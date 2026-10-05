package com.remi481.sentinel.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Entity
@Table(name = "device_metrics")
@Getter
@Setter
@NoArgsConstructor
public class DeviceMetric {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "device_uuid", nullable = false)
    private Device device;

    @Column(name = "recorded_at", nullable = false)
    private Instant recordedAt;

    @Column(name = "cpu_usage_percent")
    private Double cpuUsagePercent;

    @Column(name = "memory_used_bytes")
    private Long memoryUsedBytes;

    @Column(name = "memory_total_bytes")
    private Long memoryTotalBytes;

    @Column(name = "disk_used_bytes")
    private Long diskUsedBytes;

    @Column(name = "disk_total_bytes")
    private Long diskTotalBytes;

    @Column(name = "uptime_seconds")
    private Long uptimeSeconds;
}
