package io.github.trunone.network_tools;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.LinkProperties;
import android.net.Network;
import android.net.RouteInfo;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.net.InterfaceAddress;
import java.net.NetworkInterface;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
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
            String command = "ping -c 1 -t " + ttl + " -W " + timeout + " " + host;
            String output = executeCommand(command);

            if (output.contains("From")) {
                String ip = parseIpFromPingOutput(output);
                result.append(ttl).append("\t").append(ip).append("\n");

                if (output.contains("1 packets transmitted, 1 received")) {
                     if (output.contains("bytes from")) {
                         result.append("Destination reached.\n");
                         break;
                     }
                }
            } else if (output.contains("bytes from")) {
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
        return output.trim().replace("\n", " ");
    }

    public static String getArpTable() {
        StringBuilder result = new StringBuilder("ARP Table:\n");

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

        result.append("\nUsing 'ip neigh':\n");
        result.append(executeCommand("ip neigh"));

        return result.toString();
    }

    public static String getLocalNetworkInfo(Context context) {
        StringBuilder result = new StringBuilder();

        try {
            Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
            if (interfaces != null) {
                for (NetworkInterface networkInterface : Collections.list(interfaces)) {
                    if (networkInterface.isLoopback() || !networkInterface.isUp()) {
                        continue;
                    }

                    result.append("Interface: ").append(networkInterface.getName()).append("\n");
                    result.append("Display Name: ").append(networkInterface.getDisplayName()).append("\n");

                    byte[] mac = networkInterface.getHardwareAddress();
                    if (mac != null) {
                        StringBuilder sb = new StringBuilder();
                        for (int i = 0; i < mac.length; i++) {
                            sb.append(String.format("%02X%s", mac[i], (i < mac.length - 1) ? ":" : ""));
                        }
                        result.append("MAC: ").append(sb.toString()).append("\n");
                    }

                    List<InterfaceAddress> interfaceAddresses = networkInterface.getInterfaceAddresses();
                    for (InterfaceAddress addr : interfaceAddresses) {
                        InetAddress inetAddress = addr.getAddress();
                        result.append("IP: ").append(inetAddress.getHostAddress());

                        short prefixLength = addr.getNetworkPrefixLength();
                        result.append("/").append(prefixLength);
                        result.append("\n");
                    }
                    result.append("\n");
                }
            }
        } catch (Exception e) {
            result.append("Error getting network interfaces: ").append(e.getMessage()).append("\n");
        }

        result.append("----------------------------\n");
        result.append("Gateway Information:\n");
        if (context != null) {
            ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
            Network activeNetwork = cm.getActiveNetwork();
            if (activeNetwork != null) {
                LinkProperties lp = cm.getLinkProperties(activeNetwork);
                if (lp != null) {
                    for (RouteInfo route : lp.getRoutes()) {
                        if (route.isDefaultRoute()) {
                            result.append("Default Gateway: ").append(route.getGateway().getHostAddress()).append("\n");
                            result.append("Interface: ").append(route.getInterface()).append("\n");
                        } else {
                            result.append("Route: ").append(route.getDestination().toString())
                                    .append(" via ").append(route.getGateway() != null ? route.getGateway().getHostAddress() : "link")
                                    .append("\n");
                        }
                    }
                } else {
                    result.append("Could not retrieve link properties.\n");
                }
            } else {
                result.append("No active network found.\n");
            }
        } else {
             result.append("Context is null, cannot retrieve gateway info via ConnectivityManager.\n");
             result.append("Attempting 'ip route' (may fail on modern Android):\n");
             result.append(executeCommand("ip route"));
        }

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
