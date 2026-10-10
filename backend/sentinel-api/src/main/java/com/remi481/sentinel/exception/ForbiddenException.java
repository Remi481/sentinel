package com.remi481.sentinel.exception;

public class ForbiddenException extends RuntimeException {

    public ForbiddenException() {
        super("Device token does not match the requested device");
    }
}