package com.remi481.sentinel.exception;

public class TooManyRequestsException extends RuntimeException {

    public static final String MESSAGE = "Too many failed login attempts. Try again later.";

    private final long retryAfterSeconds;

    public TooManyRequestsException(long retryAfterSeconds) {
        super(MESSAGE);
        this.retryAfterSeconds = retryAfterSeconds;
    }

    public long getRetryAfterSeconds() {
        return retryAfterSeconds;
    }
}
