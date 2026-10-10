package com.remi481.sentinel.agent;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermission;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DeviceTokenStoreTest {

    @TempDir
    Path tempDirectory;

    @Test
    void readReturnsNullWhenFileIsMissing() {
        DeviceTokenStore store = new DeviceTokenStore(tempDirectory);
        assertNull(store.read());
    }

    @Test
    void writeThenReadRoundTrips() throws IOException {
        DeviceTokenStore store = new DeviceTokenStore(tempDirectory);
        store.write("example-device-token");

        assertEquals("example-device-token", store.read());
        assertTrue(Files.exists(tempDirectory.resolve("device-token")));
    }

    @Test
    void writeCreatesParentDirectories() throws IOException {
        Path nested = tempDirectory.resolve("nested").resolve("sentinel-agent");
        DeviceTokenStore store = new DeviceTokenStore(nested);
        store.write("example-device-token");

        assertTrue(Files.exists(nested.resolve("device-token")));
    }

    @Test
    void deleteRemovesTheFile() throws IOException {
        DeviceTokenStore store = new DeviceTokenStore(tempDirectory);
        store.write("example-device-token");
        store.delete();

        assertNull(store.read());
        assertFalse(Files.exists(tempDirectory.resolve("device-token")));
    }

    @Test
    void fileIsOwnerReadOnlyWherePosixPermissionsAreSupported() throws IOException {
        DeviceTokenStore store = new DeviceTokenStore(tempDirectory);
        if (!supportsPosix(tempDirectory)) {
            return;
        }
        store.write("example-device-token");

        Set<PosixFilePermission> permissions = Files.getPosixFilePermissions(store.path());
        assertEquals(Set.of(PosixFilePermission.OWNER_READ, PosixFilePermission.OWNER_WRITE), permissions);
    }

    private boolean supportsPosix(Path path) {
        try {
            return path.getFileSystem().supportedFileAttributeViews().contains("posix");
        } catch (UnsupportedOperationException e) {
            return false;
        }
    }
}
