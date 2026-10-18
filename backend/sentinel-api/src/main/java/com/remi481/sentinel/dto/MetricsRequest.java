package com.remi481.sentinel.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

import java.util.UUID;

public record MetricsRequest(
        @NotNull UUID uuid,
        @DecimalMin("0.0") @DecimalMax("100.0") Double cpuUsagePercent,
        @PositiveOrZero Long memoryUsedBytes,
        @PositiveOrZero Long memoryTotalBytes,
        @PositiveOrZero Long diskUsedBytes,
        @PositiveOrZero Long diskTotalBytes,
        @PositiveOrZero Long uptimeSeconds,
        @PositiveOrZero Long networkReceivedBytesPerSecond,
        @PositiveOrZero Long networkSentBytesPerSecond
) {
}
