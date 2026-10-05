package com.remi481.sentinel.agent;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

public class AgentIdentity {

    private static final Path ID_FILE =
            Path.of(System.getProperty("user.home"), ".sentinel-agent", "agent-id");

    public static UUID loadOrCreate() throws IOException {
        if (Files.exists(ID_FILE)) {
            return UUID.fromString(Files.readString(ID_FILE).trim());
        }
        UUID id = UUID.randomUUID();
        Files.createDirectories(ID_FILE.getParent());
        Files.writeString(ID_FILE, id.toString());
        return id;
    }
}
