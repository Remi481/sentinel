package com.remi481.sentinel.dto;

public record LoginResponse(String token, long expiresInSeconds) {
}