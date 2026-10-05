package com.remi481.sentinel.dto;

import java.util.UUID;

public record MetricsRequest(
        UUID uuid,
        Double cpuUsagePercent,
        Long memoryUsedBytes,
        Long memoryTotalBytes,
        Long diskUsedBytes,
        Long diskTotalBytes
) {
}

