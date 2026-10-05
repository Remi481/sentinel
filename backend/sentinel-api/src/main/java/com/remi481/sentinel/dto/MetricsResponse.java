package com.remi481.sentinel.dto;

import java.time.Instant;

public record MetricsResponse(
        Instant recordedAt,
        Double cpuUsagePercent,
        Long memoryUsedBytes,
        Long memoryTotalBytes,
        Long diskUsedBytes,
        Long diskTotalBytes
) {
}

