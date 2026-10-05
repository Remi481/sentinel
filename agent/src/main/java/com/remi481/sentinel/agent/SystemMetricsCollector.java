package com.remi481.sentinel.agent;

import java.io.File;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.nio.file.Files;
import java.nio.file.Path;

public class SystemMetricsCollector {

    private final com.sun.management.OperatingSystemMXBean osBean =
            (com.sun.management.OperatingSystemMXBean) ManagementFactory.getOperatingSystemMXBean();

    public SystemMetricsCollector() {
        osBean.getCpuLoad();
    }

    public MetricsSnapshot collect() {
        long memoryTotal = osBean.getTotalMemorySize();
        Long memoryAvailable = readLinuxAvailableMemory();
        long memoryUsed = memoryAvailable != null
                ? memoryTotal - memoryAvailable
                : memoryTotal - osBean.getFreeMemorySize();

        File root = Path.of("").toAbsolutePath().getRoot().toFile();
        long diskTotal = root.getTotalSpace();
        long diskUsed = diskTotal - root.getUsableSpace();

        return new MetricsSnapshot(readCpuPercent(), memoryUsed, memoryTotal,
                diskUsed, diskTotal, readUptimeSeconds());
    }

    private Double readCpuPercent() {
        double load = osBean.getCpuLoad();
        if (Double.isNaN(load) || load < 0) {
            return null;
        }
        return Math.round(load * 1000.0) / 10.0;
    }

    private Long readLinuxAvailableMemory() {
        Path meminfo = Path.of("/proc/meminfo");
        if (!Files.isReadable(meminfo)) {
            return null;
        }
        try {
            for (String line : Files.readAllLines(meminfo)) {
                if (line.startsWith("MemAvailable:")) {
                    String[] parts = line.trim().split("\\s+");
                    return Long.parseLong(parts[1]) * 1024;
                }
            }
        } catch (IOException | NumberFormatException e) {
            return null;
        }
        return null;
    }

    private Long readUptimeSeconds() {
        Path uptime = Path.of("/proc/uptime");
        if (!Files.isReadable(uptime)) {
            return null;
        }
        try {
            String firstValue = Files.readString(uptime).trim().split("\\s+")[0];
            return (long) Double.parseDouble(firstValue);
        } catch (IOException | NumberFormatException | ArrayIndexOutOfBoundsException e) {
            return null;
        }
    }
}