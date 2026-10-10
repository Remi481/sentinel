package com.remi481.sentinel.exception;

public class UnauthorizedException extends RuntimeException {

    public static final String MESSAGE = "Invalid or missing credentials";

    public UnauthorizedException() {
        super(MESSAGE);
    }

    public UnauthorizedException(String message) {
        super(message);
    }
}