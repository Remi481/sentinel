package com.remi481.sentinel.agent;

import java.util.UUID;

public record HeartbeatRequest(
        UUID uuid
) {
}
