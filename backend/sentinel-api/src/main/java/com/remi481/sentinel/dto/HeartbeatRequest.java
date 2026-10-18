package com.remi481.sentinel.dto;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record HeartbeatRequest(
        @NotNull UUID uuid
) {
}
