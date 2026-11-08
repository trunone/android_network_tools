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

    static String parseIpFromPingOutput(String output) {
        if (output == null) {
            return "";
        }
        try {
            String lower = output.toLowerCase();
            int fromIdx = lower.indexOf("bytes from ");
            if (fromIdx != -1) {
                String sub = output.substring(fromIdx + 11).trim();
                return extractIpCandidate(sub);
            }
            fromIdx = output.indexOf("From ");
            if (fromIdx != -1) {
                String sub = output.substring(fromIdx + 5).trim();
                return extractIpCandidate(sub);
            }
        } catch (Exception e) {
            // ignore
        }
        return output.trim().replace("\n", " ");
    }

    private static String extractIpCandidate(String sub) {
        String[] tokens = sub.split("\\s+");
        if (tokens.length == 0 || tokens[0].isEmpty()) {
            return sub;
        }
        String firstToken = tokens[0];
        if (firstToken.endsWith(":")) {
            firstToken = firstToken.substring(0, firstToken.length() - 1);
        }
        if (tokens.length > 1 && tokens[1].startsWith("(") && tokens[1].endsWith("):")) {
            return tokens[1].substring(1, tokens[1].length() - 2);
        }
        if (tokens.length > 1 && tokens[1].startsWith("(") && tokens[1].endsWith(")")) {
            return tokens[1].substring(1, tokens[1].length() - 1);
        }
        if (firstToken.startsWith("(") && firstToken.endsWith(")")) {
            firstToken = firstToken.substring(1, firstToken.length() - 1);
        }
        return firstToken;
    }

    public static String getArpTable() {
        return getArpTable(null);
    }

    public static String getArpTable(Context context) {
        StringBuilder result = new StringBuilder();
        List<String> rawArpLines = new ArrayList<>();
        boolean procArpSuccess = false;

        try (BufferedReader br = new BufferedReader(new java.io.FileReader("/proc/net/arp"))) {
            String line;
            while ((line = br.readLine()) != null) {
                rawArpLines.add(line);
            }
            procArpSuccess = true;
        } catch (Exception e) {
            // /proc/net/arp is restricted on Android 10+ (API 29+) by SELinux
        }

        String ipNeighOutput = executeCommand("ip neigh");
        if (ipNeighOutput == null || ipNeighOutput.trim().isEmpty() || ipNeighOutput.startsWith("Error")) {
            String ipNeighborOutput = executeCommand("ip neighbor");
            if (ipNeighborOutput != null && !ipNeighborOutput.trim().isEmpty() && !ipNeighborOutput.startsWith("Error")) {
                ipNeighOutput = ipNeighborOutput;
            }
        }

        result.append("--- NEIGHBOR / ARP CACHE ---\n\n");

        boolean foundEntries = false;
        if (procArpSuccess && rawArpLines.size() > 1) {
            result.append("From /proc/net/arp:\n");
            for (String l : rawArpLines) {
                result.append(l).append("\n");
            }
            result.append("\n");
            foundEntries = true;
        }

        if (ipNeighOutput != null && !ipNeighOutput.trim().isEmpty() && !ipNeighOutput.contains("Permission denied")) {
            result.append("From 'ip neigh':\n");
            result.append(ipNeighOutput.trim()).append("\n\n");
            foundEntries = true;
        }

        // Active neighbor probe for gateway and local network
        result.append("--- ACTIVE LOCAL NEIGHBORS ---\n");
        List<String> activeNeighbors = discoverLocalNeighbors(context);
        if (!activeNeighbors.isEmpty()) {
            for (String neighbor : activeNeighbors) {
                result.append(neighbor).append("\n");
            }
            foundEntries = true;
        } else {
            result.append("No active neighbors responded to local probe.\n");
        }

        if (!procArpSuccess) {
            result.append("\nNote: On Android 10+ (API 29+), direct access to /proc/net/arp is restricted by Android OS SELinux policy for privacy protection. Active neighbor discovery above probes reachable hosts directly.\n");
        }

        return result.toString();
    }

    private static List<String> discoverLocalNeighbors(Context context) {
        List<String> neighbors = new ArrayList<>();
        List<String> probeIps = new ArrayList<>();

        // 1. Identify gateway IP
        if (context != null) {
            try {
                ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
                if (cm != null) {
                    Network activeNetwork = cm.getActiveNetwork();
                    if (activeNetwork != null) {
                        LinkProperties lp = cm.getLinkProperties(activeNetwork);
                        if (lp != null) {
                            for (RouteInfo route : lp.getRoutes()) {
                                if (route.isDefaultRoute() && route.getGateway() != null) {
                                    String gw = route.getGateway().getHostAddress();
                                    if (gw != null && !probeIps.contains(gw)) {
                                        probeIps.add(gw);
                                    }
                                }
                            }
                        }
                    }
                }
            } catch (Exception ignored) {
            }
        }

        // 2. Identify local IPv4 interfaces
        String localIp = null;
        try {
            Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
            if (interfaces != null) {
                for (NetworkInterface nif : Collections.list(interfaces)) {
                    if (nif.isLoopback() || !nif.isUp()) continue;
                    for (InterfaceAddress ifAddr : nif.getInterfaceAddresses()) {
                        InetAddress addr = ifAddr.getAddress();
                        if (addr instanceof java.net.Inet4Address) {
                            localIp = addr.getHostAddress();
                            break;
                        }
                    }
                    if (localIp != null) break;
                }
            }
        } catch (Exception ignored) {
        }

        // Add common router/gateway fallback if on standard subnet
        if (localIp != null && localIp.contains(".")) {
            String subnetPrefix = localIp.substring(0, localIp.lastIndexOf('.') + 1);
            String gw1 = subnetPrefix + "1";
            if (!probeIps.contains(gw1) && !gw1.equals(localIp)) {
                probeIps.add(gw1);
            }
            String gw254 = subnetPrefix + "254";
            if (!probeIps.contains(gw254) && !gw254.equals(localIp)) {
                probeIps.add(gw254);
            }
        }

        // Probe targets via ICMP/Socket
        for (String ip : probeIps) {
            try {
                InetAddress addr = InetAddress.getByName(ip);
                boolean reachable = false;
                long start = System.currentTimeMillis();
                try {
                    reachable = addr.isReachable(500);
                } catch (Exception ignored) {
                }
                long duration = System.currentTimeMillis() - start;

                if (!reachable) {
                    // Try connecting to a common port (e.g. 53 DNS or 80 HTTP) to trigger ARP resolution
                    try (java.net.Socket s = new java.net.Socket()) {
                        s.connect(new java.net.InetSocketAddress(ip, 53), 200);
                        reachable = true;
                    } catch (Exception e) {
                        if (e.getMessage() != null && (e.getMessage().contains("refused") || e.getMessage().contains("reset"))) {
                            reachable = true; // Connection refused means host is alive and sent RST
                        }
                    }
                }

                if (reachable) {
                    String hostName = addr.getHostName();
                    neighbors.add("• " + ip + (hostName != null && !hostName.equals(ip) ? " (" + hostName + ")" : "") + " [REACHABLE]");
                }
            } catch (Exception ignored) {
            }
        }

        return neighbors;
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

    public static String dnsLookup(String host) {
        if (host == null || host.trim().isEmpty()) {
            return "Please enter a host.";
        }
        String cleanHost = host.trim();
        if (!isValidHost(cleanHost)) {
            return "Invalid host. Only alphanumeric characters, dots, hyphens, and colons are allowed.";
        }
        StringBuilder sb = new StringBuilder();
        sb.append("DNS Lookup for ").append(cleanHost).append(":\n\n");
        try {
            InetAddress[] addresses = InetAddress.getAllByName(cleanHost);
            sb.append("Found ").append(addresses.length).append(" address(es):\n");
            for (InetAddress addr : addresses) {
                sb.append("• ").append(addr.getHostAddress());
                String canonical = addr.getCanonicalHostName();
                if (canonical != null && !canonical.equalsIgnoreCase(addr.getHostAddress())) {
                    sb.append(" (").append(canonical).append(")");
                }
                sb.append("\n");
            }
        } catch (Exception e) {
            sb.append("DNS lookup failed: ").append(e.getMessage()).append("\n");
        }
        return sb.toString();
    }

    public static String scanPorts(String host, int startPort, int endPort, int timeoutMs) {
        if (host == null || host.trim().isEmpty()) {
            return "Please enter a host.";
        }
        String cleanHost = host.trim();
        if (!isValidHost(cleanHost)) {
            return "Invalid host. Only alphanumeric characters, dots, hyphens, and colons are allowed.";
        }
        if (startPort < 1 || startPort > 65535 || endPort < 1 || endPort > 65535 || startPort > endPort) {
            return "Invalid port range. Ports must be between 1 and 65535, with start <= end.";
        }
        if (endPort - startPort > 1000) {
            return "Port range too large. Maximum 1000 ports at a time.";
        }
        if (timeoutMs <= 0) {
            timeoutMs = 200;
        }

        StringBuilder sb = new StringBuilder();
        sb.append("Scanning ").append(cleanHost).append(" (ports ").append(startPort).append("-").append(endPort).append(")...\n\n");
        int openCount = 0;

        for (int port = startPort; port <= endPort; port++) {
            if (Thread.currentThread().isInterrupted()) {
                sb.append("\nScan interrupted.\n");
                break;
            }
            try (java.net.Socket socket = new java.net.Socket()) {
                socket.connect(new java.net.InetSocketAddress(cleanHost, port), timeoutMs);
                sb.append("Port ").append(port).append(" is OPEN\n");
                openCount++;
            } catch (Exception e) {
                // Closed or filtered
            }
        }
        sb.append("\nScan complete. ").append(openCount).append(" open port(s) found.\n");
        return sb.toString();
    }

    public static String calculateSubnet(String ip, int prefixLength) {
        if (ip == null || ip.trim().isEmpty()) {
            return "Please enter an IP address.";
        }
        String cleanIp = ip.trim();
        if (prefixLength < 0 || prefixLength > 32) {
            return "Invalid prefix length. Must be between 0 and 32.";
        }

        String[] parts = cleanIp.split("\\.");
        if (parts.length != 4) {
            return "Invalid IPv4 address format. Subnet calculator supports IPv4 (e.g. 192.168.1.1).";
        }

        int ipInt = 0;
        for (int i = 0; i < 4; i++) {
            try {
                int octet = Integer.parseInt(parts[i]);
                if (octet < 0 || octet > 255) {
                    return "Invalid octet: " + octet + ". Must be 0-255.";
                }
                ipInt = (ipInt << 8) | octet;
            } catch (NumberFormatException e) {
                return "Invalid IPv4 address: " + cleanIp;
            }
        }

        int netmaskInt = prefixLength == 0 ? 0 : (0xFFFFFFFF << (32 - prefixLength));
        int wildcardInt = ~netmaskInt;
        int networkInt = ipInt & netmaskInt;
        int broadcastInt = networkInt | wildcardInt;

        String netmaskStr = intToIpv4(netmaskInt);
        String wildcardStr = intToIpv4(wildcardInt);
        String networkStr = intToIpv4(networkInt);
        String broadcastStr = intToIpv4(broadcastInt);

        long totalHosts = prefixLength == 32 ? 1 : (1L << (32 - prefixLength));
        long usableHosts = prefixLength >= 31 ? (prefixLength == 31 ? 2 : 1) : Math.max(0, totalHosts - 2);

        String firstUsable;
        String lastUsable;
        if (prefixLength == 32) {
            firstUsable = networkStr;
            lastUsable = networkStr;
        } else if (prefixLength == 31) {
            firstUsable = networkStr;
            lastUsable = broadcastStr;
        } else {
            firstUsable = intToIpv4(networkInt + 1);
            lastUsable = intToIpv4(broadcastInt - 1);
        }

        StringBuilder sb = new StringBuilder();
        sb.append("IP Address: ").append(cleanIp).append("\n");
        sb.append("Prefix Length: /").append(prefixLength).append("\n");
        sb.append("Subnet Mask: ").append(netmaskStr).append("\n");
        sb.append("Wildcard Mask: ").append(wildcardStr).append("\n");
        sb.append("Network Address: ").append(networkStr).append("\n");
        sb.append("Broadcast Address: ").append(broadcastStr).append("\n");
        sb.append("Usable Host Range: ").append(firstUsable).append(" - ").append(lastUsable).append("\n");
        sb.append("Total Hosts: ").append(totalHosts).append("\n");
        sb.append("Usable Hosts: ").append(usableHosts).append("\n");

        return sb.toString();
    }

    private static String intToIpv4(int val) {
        return ((val >>> 24) & 0xFF) + "." +
               ((val >>> 16) & 0xFF) + "." +
               ((val >>> 8) & 0xFF) + "." +
               (val & 0xFF);
    }

    public static String sendWakeOnLan(String macStr, String broadcastIp, int port) {
        if (macStr == null || macStr.trim().isEmpty()) {
            return "Please enter a MAC address.";
        }
        String cleanMac = macStr.trim();
        byte[] macBytes = parseMac(cleanMac);
        if (macBytes == null) {
            return "Invalid MAC address. Expected format: XX:XX:XX:XX:XX:XX or XX-XX-XX-XX-XX-XX.";
        }

        String targetIp = (broadcastIp == null || broadcastIp.trim().isEmpty()) ? "255.255.255.255" : broadcastIp.trim();
        if (port <= 0 || port > 65535) {
            port = 9; // standard WOL port
        }

        try {
            // Magic packet = 6 bytes of 0xFF followed by 16 repetitions of target MAC (102 bytes total)
            byte[] bytes = new byte[6 + 16 * macBytes.length];
            for (int i = 0; i < 6; i++) {
                bytes[i] = (byte) 0xFF;
            }
            for (int i = 6; i < bytes.length; i += macBytes.length) {
                System.arraycopy(macBytes, 0, bytes, i, macBytes.length);
            }

            InetAddress address = InetAddress.getByName(targetIp);
            java.net.DatagramPacket packet = new java.net.DatagramPacket(bytes, bytes.length, address, port);
            try (java.net.DatagramSocket socket = new java.net.DatagramSocket()) {
                socket.setBroadcast(true);
                socket.send(packet);
            }

            return "Wake-on-LAN magic packet sent successfully to " + cleanMac + " via " + targetIp + ":" + port + ".";
        } catch (Exception e) {
            return "Failed to send Wake-on-LAN packet: " + e.getMessage();
        }
    }

    public static byte[] parseMac(String macStr) {
        if (macStr == null) {
            return null;
        }
        String[] parts = macStr.split("[:\\-]");
        if (parts.length != 6) {
            return null;
        }
        byte[] mac = new byte[6];
        try {
            for (int i = 0; i < 6; i++) {
                if (parts[i].length() == 0 || parts[i].length() > 2) {
                    return null;
                }
                mac[i] = (byte) Integer.parseInt(parts[i], 16);
            }
            return mac;
        } catch (NumberFormatException e) {
            return null;
        }
    }

    public static String getIpGeoInfo(String query) {
        String target = (query == null) ? "" : query.trim();
        if (!target.isEmpty() && !isValidHost(target)) {
            return "Invalid host or IP. Only alphanumeric characters, dots, hyphens, and colons are allowed.";
        }

        String urlStr = target.isEmpty() ? "https://ipwho.is/" : "https://ipwho.is/" + target;
        try {
            java.net.URL url = new java.net.URL(urlStr);
            java.net.HttpURLConnection conn = (java.net.HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(6000);
            conn.setReadTimeout(6000);
            conn.setRequestProperty("User-Agent", "Mozilla/5.0 NetworkTools/1.0");

            int responseCode = conn.getResponseCode();
            if (responseCode != 200) {
                return "HTTP Error: Received response code " + responseCode;
            }

            BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            StringBuilder response = new StringBuilder();
            String inputLine;
            while ((inputLine = in.readLine()) != null) {
                response.append(inputLine);
            }
            in.close();

            return formatIpGeoJson(response.toString());
        } catch (Exception e) {
            return "Failed to fetch IP Geolocation information: " + e.getMessage();
        }
    }

    static String formatIpGeoJson(String jsonStr) {
        try {
            org.json.JSONObject obj = new org.json.JSONObject(jsonStr);
            if (!obj.optBoolean("success", true) && obj.has("message")) {
                return "Error: " + obj.optString("message");
            }

            StringBuilder sb = new StringBuilder();
            sb.append("IP Address:   ").append(obj.optString("ip", "N/A")).append("\n");
            sb.append("Type:         ").append(obj.optString("type", "N/A")).append("\n");
            sb.append("Continent:    ").append(obj.optString("continent", "N/A"))
              .append(" (").append(obj.optString("continent_code", "")).append(")\n");
            sb.append("Country:      ").append(obj.optString("country", "N/A"))
              .append(" (").append(obj.optString("country_code", "")).append(")\n");
            sb.append("Region/State: ").append(obj.optString("region", "N/A")).append("\n");
            sb.append("City:         ").append(obj.optString("city", "N/A")).append("\n");
            sb.append("Postal Code:  ").append(obj.optString("postal", "N/A")).append("\n");
            sb.append("Coordinates:  ").append(obj.optDouble("latitude", 0.0))
              .append(", ").append(obj.optDouble("longitude", 0.0)).append("\n");

            org.json.JSONObject conn = obj.optJSONObject("connection");
            if (conn != null) {
                sb.append("ASN:          AS").append(conn.opt("asn")).append("\n");
                sb.append("ISP:          ").append(conn.optString("isp", "N/A")).append("\n");
                sb.append("Organization: ").append(conn.optString("org", "N/A")).append("\n");
            }

            org.json.JSONObject tz = obj.optJSONObject("timezone");
            if (tz != null) {
                sb.append("Timezone:     ").append(tz.optString("id", "N/A"))
                  .append(" (").append(tz.optString("utc", "")).append(")\n");
            }

            return sb.toString();
        } catch (Exception e) {
            return jsonStr;
        }
    }

    public static String whois(String query) {
        if (query == null || query.trim().isEmpty()) {
            return "Please enter a domain or IP address.";
        }
        String cleanQuery = query.trim().toLowerCase();
        if (cleanQuery.startsWith("http://")) cleanQuery = cleanQuery.substring(7);
        if (cleanQuery.startsWith("https://")) cleanQuery = cleanQuery.substring(8);
        int slashIdx = cleanQuery.indexOf('/');
        if (slashIdx != -1) cleanQuery = cleanQuery.substring(0, slashIdx);

        if (!isValidHost(cleanQuery)) {
            return "Invalid host or IP. Only alphanumeric characters, dots, hyphens, and colons are allowed.";
        }

        // Determine default WHOIS server
        String whoisServer = "whois.iana.org";
        if (cleanQuery.matches(".*\\.(com|net)$")) {
            whoisServer = "whois.verisign-grs.com";
        } else if (cleanQuery.matches(".*\\.org$")) {
            whoisServer = "whois.pir.org";
        } else if (cleanQuery.matches(".*\\.io$")) {
            whoisServer = "whois.nic.io";
        } else if (cleanQuery.matches("^[0-9.]+$") || cleanQuery.contains(":")) {
            whoisServer = "whois.arin.net";
        }

        String response = queryWhoisServer(whoisServer, cleanQuery);
        // Check for referral server
        String referralServer = extractReferralServer(response);
        if (referralServer != null && !referralServer.equalsIgnoreCase(whoisServer)) {
            String referralResponse = queryWhoisServer(referralServer, cleanQuery);
            if (referralResponse != null && !referralResponse.trim().isEmpty() && !referralResponse.startsWith("Error")) {
                return "Query: " + cleanQuery + " (Server: " + referralServer + ")\n\n" + referralResponse;
            }
        }

        return "Query: " + cleanQuery + " (Server: " + whoisServer + ")\n\n" + response;
    }

    static String extractReferralServer(String whoisResponse) {
        if (whoisResponse == null) return null;
        for (String line : whoisResponse.split("\n")) {
            String lower = line.toLowerCase();
            if (lower.startsWith("whois:") || lower.startsWith("refer:") || lower.startsWith("registrant whois server:")) {
                int colonIdx = line.indexOf(':');
                if (colonIdx != -1) {
                    String server = line.substring(colonIdx + 1).trim();
                    if (server.startsWith("http://") || server.startsWith("https://")) {
                        continue;
                    }
                    if (isValidHost(server)) {
                        return server;
                    }
                }
            }
        }
        return null;
    }

    private static String queryWhoisServer(String server, String query) {
        StringBuilder sb = new StringBuilder();
        try (java.net.Socket socket = new java.net.Socket()) {
            socket.connect(new java.net.InetSocketAddress(server, 43), 7000);
            socket.setSoTimeout(7000);

            java.io.OutputStream out = socket.getOutputStream();
            out.write((query + "\r\n").getBytes());
            out.flush();

            BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line).append("\n");
            }
            return sb.toString();
        } catch (Exception e) {
            return "Error querying WHOIS server (" + server + "): " + e.getMessage();
        }
    }

    public static String httpInspect(String urlInput) {
        if (urlInput == null || urlInput.trim().isEmpty()) {
            return "Please enter a URL or host.";
        }
        String cleanUrl = urlInput.trim();
        if (!cleanUrl.startsWith("http://") && !cleanUrl.startsWith("https://")) {
            cleanUrl = "https://" + cleanUrl;
        }

        StringBuilder sb = new StringBuilder();
        sb.append("Inspecting: ").append(cleanUrl).append("\n\n");
        long startTime = System.currentTimeMillis();

        try {
            java.net.URL url = new java.net.URL(cleanUrl);
            java.net.HttpURLConnection conn = (java.net.HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setInstanceFollowRedirects(false);
            conn.setConnectTimeout(8000);
            conn.setReadTimeout(8000);
            conn.setRequestProperty("User-Agent", "Mozilla/5.0 (Android NetworkTools/1.0)");

            conn.connect();
            long latency = System.currentTimeMillis() - startTime;
            int responseCode = conn.getResponseCode();
            String responseMessage = conn.getResponseMessage();

            sb.append("Response: ").append(responseCode).append(" ").append(responseMessage != null ? responseMessage : "").append("\n");
            sb.append("Latency:  ").append(latency).append(" ms\n\n");
            sb.append("--- Headers ---\n");

            java.util.Map<String, List<String>> headers = conn.getHeaderFields();
            for (java.util.Map.Entry<String, List<String>> entry : headers.entrySet()) {
                String key = entry.getKey();
                for (String val : entry.getValue()) {
                    if (key == null) {
                        sb.append(val).append("\n");
                    } else {
                        sb.append(key).append(": ").append(val).append("\n");
                    }
                }
            }

            conn.disconnect();
            return sb.toString();
        } catch (Exception e) {
            return sb.append("Request failed: ").append(e.getMessage()).toString();
        }
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
