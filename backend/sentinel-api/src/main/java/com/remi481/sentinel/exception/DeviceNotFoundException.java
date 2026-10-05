package com.remi481.sentinel.exception;

import java.util.UUID;

public class DeviceNotFoundException extends RuntimeException {

    public DeviceNotFoundException(UUID uuid) {
        super("Device not found: " + uuid);
    }
}
