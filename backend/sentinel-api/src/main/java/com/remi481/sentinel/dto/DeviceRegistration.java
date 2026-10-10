package com.remi481.sentinel.dto;

import com.remi481.sentinel.entity.Device;

public record DeviceRegistration(Device device, String deviceToken) {
}