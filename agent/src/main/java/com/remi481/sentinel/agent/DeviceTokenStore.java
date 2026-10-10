package com.remi481.sentinel.agent;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.PosixFilePermission;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.Set;

public class DeviceTokenStore {

    private static final Set<PosixFilePermission> OWNER_ONLY =
            PosixFilePermissions.fromString("rw-------");

    private final Path tokenFile;

    public DeviceTokenStore() {
        this(defaultDirectory());
    }

    public DeviceTokenStore(Path directory) {
        this.tokenFile = directory.resolve("device-token");
    }

    public static Path defaultDirectory() {
        return Paths.get(System.getProperty("user.home"), ".sentinel-agent");
    }

    public Path path() {
        return tokenFile;
    }

    public String read() {
        if (!Files.exists(tokenFile)) {
            return null;
        }
        try {
            String token = Files.readString(tokenFile).trim();
            return token.isEmpty() ? null : token;
        } catch (IOException e) {
            return null;
        }
    }

    public void write(String token) throws IOException {
        Path parent = tokenFile.getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }
        Files.writeString(tokenFile, token);
        restrictToOwner();
    }

    public void delete() throws IOException {
        Files.deleteIfExists(tokenFile);
    }

    private void restrictToOwner() {
        try {
            Files.setPosixFilePermissions(tokenFile, OWNER_ONLY);
        } catch (UnsupportedOperationException | IOException e) {
            // Windows and other platforms without POSIX permissions: best effort, nothing to do.
        }
    }
}
