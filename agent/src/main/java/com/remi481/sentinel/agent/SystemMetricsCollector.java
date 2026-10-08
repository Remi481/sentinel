package com.remi481.sentinel.agent;

import java.io.File;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class SystemMetricsCollector {

    private static final boolean IS_WINDOWS =
            System.getProperty("os.name", "").startsWith("Windows");

    private final com.sun.management.OperatingSystemMXBean osBean =
            (com.sun.management.OperatingSystemMXBean) ManagementFactory.getOperatingSystemMXBean();

    private final Instant windowsBootTime;
    private long[] previousNetwork;
    private long previousNetworkNanos;

    public SystemMetricsCollector() {
        // The first CPU reading is often unavailable, so take one now to warm it up.
        osBean.getCpuLoad();
        // On Windows the boot time is read once and uptime is derived from it.
        windowsBootTime = IS_WINDOWS ? readWindowsBootTime() : null;
        // Take a network baseline so the first real reading already has a rate.
        previousNetwork = readNetworkTotals();
        previousNetworkNanos = System.nanoTime();
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

        Long[] network = readNetworkRates();

        return new MetricsSnapshot(readCpuPercent(), memoryUsed, memoryTotal,
                diskUsed, diskTotal, readUptimeSeconds(), network[0], network[1]);
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
        if (IS_WINDOWS) {
            if (windowsBootTime == null) {
                return null;
            }
            return Duration.between(windowsBootTime, Instant.now()).getSeconds();
        }
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

    private Long[] readNetworkRates() {
        long[] current = readNetworkTotals();
        long now = System.nanoTime();
        long[] previous = previousNetwork;
        long previousNanos = previousNetworkNanos;

        previousNetwork = current;
        previousNetworkNanos = now;

        if (current == null || previous == null) {
            return new Long[]{null, null};
        }
        double seconds = (now - previousNanos) / 1_000_000_000.0;
        if (seconds <= 0 || current[0] < previous[0] || current[1] < previous[1]) {
            return new Long[]{null, null};
        }
        return new Long[]{
                Math.round((current[0] - previous[0]) / seconds),
                Math.round((current[1] - previous[1]) / seconds)
        };
    }

    private long[] readNetworkTotals() {
        if (IS_WINDOWS) {
            return readWindowsNetworkTotals();
        }
        Path netDev = Path.of("/proc/net/dev");
        if (!Files.isReadable(netDev)) {
            return null;
        }
        try {
            long received = 0;
            long sent = 0;
            List<String> lines = Files.readAllLines(netDev);
            for (int i = 2; i < lines.size(); i++) {
                String line = lines.get(i);
                int colon = line.indexOf(':');
                if (colon < 0) {
                    continue;
                }
                String name = line.substring(0, colon).trim();
                if (isVirtualInterface(name)) {
                    continue;
                }
                String[] parts = line.substring(colon + 1).trim().split("\\s+");
                received += Long.parseLong(parts[0]);
                sent += Long.parseLong(parts[8]);
            }
            return new long[]{received, sent};
        } catch (IOException | NumberFormatException | ArrayIndexOutOfBoundsException e) {
            return null;
        }
    }

    private Instant readWindowsBootTime() {
        String output = runPowerShell("(Get-CimInstance Win32_OperatingSystem)"
                + ".LastBootUpTime.ToUniversalTime().ToString('o')");
        if (output == null) {
            return null;
        }
        try {
            return Instant.parse(output.trim());
        } catch (DateTimeParseException e) {
            return null;
        }
    }

    private long[] readWindowsNetworkTotals() {
        // Get-NetAdapterStatistics is denied to normal users here, use perf counters.
        String output = runPowerShell("$physical = Get-CimInstance -Namespace root/StandardCimv2 "
                + "-ClassName MSFT_NetAdapter | Where-Object { $_.HardwareInterface } "
                + "| ForEach-Object { $_.InterfaceDescription }; "
                + "Get-CimInstance Win32_PerfRawData_Tcpip_NetworkInterface "
                + "| Where-Object { $physical -contains $_.Name } "
                + "| ForEach-Object { '{0} {1}' -f $_.BytesReceivedPerSec, $_.BytesSentPerSec }");
        if (output == null) {
            return null;
        }
        try {
            long received = 0;
            long sent = 0;
            boolean found = false;
            for (String line : output.split("\\R")) {
                if (line.isBlank()) {
                    continue;
                }
                String[] parts = line.trim().split("\\s+");
                received += Long.parseLong(parts[0]);
                sent += Long.parseLong(parts[1]);
                found = true;
            }
            return found ? new long[]{received, sent} : null;
        } catch (NumberFormatException | ArrayIndexOutOfBoundsException e) {
            return null;
        }
    }

    private String runPowerShell(String command) {
        Path outputFile = null;
        try {
            // Write stdout to a file so a chatty child cannot block us.
            outputFile = Files.createTempFile("sentinel-powershell", ".txt");
            Process process = new ProcessBuilder(
                    "powershell", "-NoProfile", "-NonInteractive", "-Command", command)
                    .redirectOutput(outputFile.toFile())
                    .redirectError(ProcessBuilder.Redirect.DISCARD)
                    .start();
            if (!process.waitFor(10, TimeUnit.SECONDS)) {
                process.destroyForcibly();
                return null;
            }
            if (process.exitValue() != 0) {
                return null;
            }
            return Files.readString(outputFile, StandardCharsets.UTF_8);
        } catch (IOException e) {
            return null;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return null;
        } finally {
            if (outputFile != null) {
                try {
                    Files.deleteIfExists(outputFile);
                } catch (IOException e) {
                    // Ignore cleanup failure.
                }
            }
        }
    }

    private boolean isVirtualInterface(String name) {
        return name.equals("lo")
                || name.startsWith("docker")
                || name.startsWith("veth")
                || name.startsWith("br-");
    }
}