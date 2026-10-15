package com.remi481.sentinel.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record LoginRequest(
        @NotBlank @Size(max = 200) String username,
        @NotBlank @Size(max = 200) String password) {
}
