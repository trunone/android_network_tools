package com.example.networktools;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

public class NetworkUtils {

    public static String ping(String host) {
        return ping(host, 4, 1);
    }

    public static String ping(String host, int count, int timeout) {
        if (host == null || host.trim().isEmpty()) {
            return "Please enter a host.";
        }
        if (!isValidHost(host)) {
            return "Invalid host. Only alphanumeric characters, dots, hyphens, and colons are allowed.";
        }
        return executeCommand("ping -c " + count + " -W " + timeout + " " + host);
    }

    public static String traceroute(String host) {
        return traceroute(host, 30, 1);
    }

    public static String traceroute(String host, int maxHops, int timeout) {
        if (host == null || host.trim().isEmpty()) {
            return "Please enter a host.";
        }
        if (!isValidHost(host)) {
            return "Invalid host. Only alphanumeric characters, dots, hyphens, and colons are allowed.";
        }
        StringBuilder result = new StringBuilder();
        result.append("Traceroute to ").append(host).append("\n");

        for (int ttl = 1; ttl <= maxHops; ttl++) {
            // Use ping with TTL. -c 1 (count 1), -t ttl, -W timeout
            String command = "ping -c 1 -t " + ttl + " -W " + timeout + " " + host;
            String output = executeCommand(command);

            if (output.contains("From")) {
                // Parse the IP from "From 192.168.1.1: icmp_seq=1 Time to live exceeded"
                // This is a rough heuristic.
                String ip = parseIpFromPingOutput(output);
                result.append(ttl).append("\t").append(ip).append("\n");

                if (output.contains("1 packets transmitted, 1 received")) {
                     // We reached the destination (though -t usually results in error if TTL expired)
                     // If we actually reached the target, ping exits with 0 and standard output
                     // Wait, if TTL is enough, ping succeeds.
                     // But we want to know if we hit the target.
                     // If the output does NOT contain "Time to live exceeded", and contains "bytes from", we reached it.
                     if (output.contains("bytes from")) {
                         result.append("Destination reached.\n");
                         break;
                     }
                }
            } else if (output.contains("bytes from")) {
                 // Reached destination
                 String ip = parseIpFromPingOutput(output);
                 result.append(ttl).append("\t").append(ip).append("\n");
                 break;
            } else {
                result.append(ttl).append("\t*\n");
            }
        }
        return result.toString();
    }

    private static String parseIpFromPingOutput(String output) {
        try {
            if (output.contains("From")) {
                int fromIndex = output.indexOf("From");
                int colonIndex = output.indexOf(":", fromIndex);
                if (colonIndex > fromIndex) {
                    String sub = output.substring(fromIndex + 5, colonIndex);
                    // Sometimes it says "From 1.2.3.4 (1.2.3.4)" or just "From 1.2.3.4"
                    return sub.trim();
                }
            }
            if (output.contains("bytes from")) {
                 int fromIndex = output.indexOf("bytes from");
                 int colonIndex = output.indexOf(":", fromIndex);
                 if (colonIndex > fromIndex) {
                     return output.substring(fromIndex + 11, colonIndex).trim();
                 }
            }
        } catch (Exception e) {
            // ignore
        }
        return output.trim().replace("\n", " "); // Return raw if parsing fails
    }

    public static String getArpTable() {
        StringBuilder result = new StringBuilder("ARP Table:\n");

        // Method 1: /proc/net/arp
        try {
            BufferedReader br = new BufferedReader(new java.io.FileReader("/proc/net/arp"));
            String line;
            while ((line = br.readLine()) != null) {
                result.append(line).append("\n");
            }
            br.close();
        } catch (IOException e) {
            result.append("Failed to read /proc/net/arp: ").append(e.getMessage()).append("\n");
        }

        // Method 2: ip neigh
        result.append("\nUsing 'ip neigh':\n");
        result.append(executeCommand("ip neigh"));

        return result.toString();
    }

    private static boolean isValidHost(String host) {
        return host.matches("^[a-zA-Z0-9.:-]+$");
    }

    private static String executeCommand(String command) {
        StringBuilder output = new StringBuilder();
        try {
            Process process = Runtime.getRuntime().exec(command);
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String line;
            while ((line = reader.readLine()) != null) {
                output.append(line).append("\n");
            }

            // Also read error stream
            BufferedReader errorReader = new BufferedReader(new InputStreamReader(process.getErrorStream()));
            while ((line = errorReader.readLine()) != null) {
                output.append(line).append("\n");
            }

            process.waitFor();
        } catch (Exception e) {
            output.append("Error executing command: ").append(e.getMessage());
        }
        return output.toString();
    }
}
