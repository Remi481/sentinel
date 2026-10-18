package com.remi481.sentinel.entity;

import com.remi481.sentinel.repository.DeviceRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.dao.DataIntegrityViolationException;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@DataJpaTest(properties =
        "spring.datasource.url=jdbc:h2:mem:token-index-test;MODE=PostgreSQL;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE")
class DeviceTokenHashIndexTest {

    @Autowired
    private DeviceRepository deviceRepository;

    @Test
    void manyDevicesMayHaveANullTokenHash() {
        deviceRepository.saveAndFlush(device(null));
        deviceRepository.saveAndFlush(device(null));
        deviceRepository.saveAndFlush(device(null));

        assertEquals(3, deviceRepository.count());
    }

    @Test
    void twoDevicesMayNotShareATokenHash() {
        deviceRepository.saveAndFlush(device("a".repeat(64)));

        assertThrows(DataIntegrityViolationException.class,
                () -> deviceRepository.saveAndFlush(device("a".repeat(64))));
    }

    private Device device(String tokenHash) {
        Device device = new Device();
        device.setUuid(UUID.randomUUID());
        device.setHostname("index-test-host");
        device.setStatus(DeviceStatus.ONLINE);
        device.setTokenHash(tokenHash);
        return device;
    }
}
