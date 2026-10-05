package com.remi481.sentinel.agent;

public record MetricsSnapshot(
        Double cpuUsagePercent,
        Long memoryUsedBytes,
        Long memoryTotalBytes,
        Long diskUsedBytes,
        Long diskTotalBytes
) {
}
