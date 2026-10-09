package com.remi481.sentinel.scheduling;

import com.remi481.sentinel.service.DeviceService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OfflineDeviceCheckerTest {

    @Mock
    private DeviceService deviceService;

    @Test
    void delegatesToService() {
        when(deviceService.markStaleDevicesOffline()).thenReturn(2);

        new OfflineDeviceChecker(deviceService).checkOfflineDevices();

        verify(deviceService).markStaleDevicesOffline();
    }
}
