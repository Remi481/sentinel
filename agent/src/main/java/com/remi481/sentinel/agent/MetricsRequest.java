package com.remi481.sentinel.agent;

import java.util.UUID;

public record MetricsRequest(
        UUID uuid,
        Double cpuUsagePercent,
        Long memoryUsedBytes,
        Long memoryTotalBytes,
        Long diskUsedBytes,
        Long diskTotalBytes,
        Long uptimeSeconds,
        Long networkReceivedBytesPerSecond,
        Long networkSentBytesPerSecond
) {

    public static MetricsRequest of(UUID uuid, MetricsSnapshot snapshot) {
        return new MetricsRequest(
                uuid,
                snapshot.cpuUsagePercent(),
                snapshot.memoryUsedBytes(),
                snapshot.memoryTotalBytes(),
                snapshot.diskUsedBytes(),
                snapshot.diskTotalBytes(),
                snapshot.uptimeSeconds(),
                snapshot.networkReceivedBytesPerSecond(),
                snapshot.networkSentBytesPerSecond());
    }
}