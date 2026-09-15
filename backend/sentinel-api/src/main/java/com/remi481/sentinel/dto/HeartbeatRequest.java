package com.remi481.sentinel.dto;

import java.util.UUID;

public record HeartbeatRequest(
        UUID uuid
) {
}
