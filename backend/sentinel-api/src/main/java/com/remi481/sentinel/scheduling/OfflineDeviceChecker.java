package com.remi481.sentinel.scheduling;

import com.remi481.sentinel.service.DeviceService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class OfflineDeviceChecker {

    private static final Logger log = LoggerFactory.getLogger(OfflineDeviceChecker.class);

    private final DeviceService deviceService;

    public OfflineDeviceChecker(DeviceService deviceService) {
        this.deviceService = deviceService;
    }

    @Scheduled(fixedDelayString = "${sentinel.offline.check-interval-ms}")
    public void checkOfflineDevices() {
        int markedOffline = deviceService.markStaleDevicesOffline();
        if (markedOffline > 0) {
            log.info("Marked {} devices offline", markedOffline);
        }
    }
}
