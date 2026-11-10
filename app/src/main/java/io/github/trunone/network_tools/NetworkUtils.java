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

    public interface SpeedTestListener {
        void onProgress(String message);
    }

    public static String getSslCertificateInfo(String host, int port, int timeoutMs) {
        if (host == null || host.trim().isEmpty()) {
            return "Please enter a host.";
        }
        String cleanHost = host.trim();
        // Remove scheme if entered by user (e.g. https://)
        if (cleanHost.startsWith("https://")) {
            cleanHost = cleanHost.substring(8);
        } else if (cleanHost.startsWith("http://")) {
            cleanHost = cleanHost.substring(7);
        }
        int slashIdx = cleanHost.indexOf('/');
        if (slashIdx != -1) {
            cleanHost = cleanHost.substring(0, slashIdx);
        }
        int colonIdx = cleanHost.indexOf(':');
        if (colonIdx != -1) {
            try {
                port = Integer.parseInt(cleanHost.substring(colonIdx + 1));
            } catch (Exception ignored) {
            }
            cleanHost = cleanHost.substring(0, colonIdx);
        }

        if (!isValidHost(cleanHost)) {
            return "Invalid host. Only alphanumeric characters, dots, hyphens, and colons are allowed.";
        }
        if (port <= 0 || port > 65535) {
            port = 443;
        }
        if (timeoutMs <= 0) {
            timeoutMs = 10000;
        }

        StringBuilder sb = new StringBuilder();
        sb.append("SSL/TLS Certificate Inspection: ").append(cleanHost).append(":").append(port).append("\n");
        sb.append("====================================================\n\n");

        try {
            javax.net.ssl.SSLContext sslContext = javax.net.ssl.SSLContext.getInstance("TLS");
            sslContext.init(null, null, new java.security.SecureRandom());
            javax.net.ssl.SSLSocketFactory factory = sslContext.getSocketFactory();

            try (javax.net.ssl.SSLSocket sslSocket = (javax.net.ssl.SSLSocket) factory.createSocket()) {
                sslSocket.connect(new java.net.InetSocketAddress(cleanHost, port), timeoutMs);
                sslSocket.setSoTimeout(timeoutMs);
                sslSocket.startHandshake();

                javax.net.ssl.SSLSession session = sslSocket.getSession();
                sb.append("Protocol:      ").append(session.getProtocol()).append("\n");
                sb.append("Cipher Suite:  ").append(session.getCipherSuite()).append("\n\n");

                java.security.cert.Certificate[] certs = session.getPeerCertificates();
                sb.append("Certificate Chain (Length: ").append(certs.length).append("):\n");
                sb.append("----------------------------------------------------\n");

                for (int i = 0; i < certs.length; i++) {
                    sb.append("\n[Certificate #").append(i + 1).append("]\n");
                    if (certs[i] instanceof java.security.cert.X509Certificate) {
                        java.security.cert.X509Certificate x509 = (java.security.cert.X509Certificate) certs[i];
                        sb.append("Subject:       ").append(x509.getSubjectX500Principal().getName()).append("\n");
                        sb.append("Issuer:        ").append(x509.getIssuerX500Principal().getName()).append("\n");
                        sb.append("Valid From:    ").append(x509.getNotBefore()).append("\n");
                        sb.append("Valid Until:   ").append(x509.getNotAfter()).append("\n");

                        long diffMs = x509.getNotAfter().getTime() - System.currentTimeMillis();
                        long daysLeft = java.util.concurrent.TimeUnit.MILLISECONDS.toDays(diffMs);
                        if (daysLeft < 0) {
                            sb.append("Status:        EXPIRED (").append(Math.abs(daysLeft)).append(" days ago)\n");
                        } else {
                            sb.append("Expires In:    ").append(daysLeft).append(" day(s)\n");
                        }

                        sb.append("Serial Number: ").append(x509.getSerialNumber().toString(16).toUpperCase()).append("\n");
                        sb.append("Sig Algorithm: ").append(x509.getSigAlgName()).append("\n");

                        // Fingerprints
                        byte[] encoded = x509.getEncoded();
                        sb.append("SHA-256:       ").append(getFingerprint(encoded, "SHA-256")).append("\n");
                        sb.append("SHA-1:         ").append(getFingerprint(encoded, "SHA-1")).append("\n");

                        // Subject Alternative Names (SANs)
                        try {
                            java.util.Collection<List<?>> sans = x509.getSubjectAlternativeNames();
                            if (sans != null && !sans.isEmpty()) {
                                sb.append("Subject Alternative Names (SAN):\n");
                                for (List<?> sanItem : sans) {
                                    if (sanItem.size() >= 2) {
                                        sb.append("  • ").append(sanItem.get(1)).append("\n");
                                    }
                                }
                            }
                        } catch (Exception ignored) {
                        }
                    } else {
                        sb.append("Type: ").append(certs[i].getType()).append("\n");
                    }
                }
            }
            return sb.toString();
        } catch (Exception e) {
            sb.append("SSL/TLS inspection failed: ").append(e.getMessage()).append("\n");
            return sb.toString();
        }
    }

    private static String getFingerprint(byte[] certBytes, String algorithm) {
        try {
            java.security.MessageDigest md = java.security.MessageDigest.getInstance(algorithm);
            byte[] digest = md.digest(certBytes);
            StringBuilder hex = new StringBuilder();
            for (int i = 0; i < digest.length; i++) {
                hex.append(String.format("%02X", digest[i]));
                if (i < digest.length - 1) {
                    hex.append(":");
                }
            }
            return hex.toString();
        } catch (Exception e) {
            return "Unavailable";
        }
    }

    public static String queryDnsRecords(String host, String recordType) {
        if (host == null || host.trim().isEmpty()) {
            return "Please enter a host or domain name.";
        }
        String cleanHost = host.trim();
        if (cleanHost.startsWith("https://")) cleanHost = cleanHost.substring(8);
        if (cleanHost.startsWith("http://")) cleanHost = cleanHost.substring(7);
        int slashIdx = cleanHost.indexOf('/');
        if (slashIdx != -1) cleanHost = cleanHost.substring(0, slashIdx);

        if (!isValidHost(cleanHost)) {
            return "Invalid host. Only alphanumeric characters, dots, hyphens, and colons are allowed.";
        }

        String type = (recordType == null || recordType.trim().isEmpty()) ? "A" : recordType.trim().toUpperCase();

        StringBuilder sb = new StringBuilder();
        sb.append("DNS-over-HTTPS Records for ").append(cleanHost).append(" [Type: ").append(type).append("]\n");
        sb.append("====================================================\n\n");

        try {
            String endpoint = "https://dns.google/resolve?name=" + cleanHost + "&type=" + type;
            java.net.URL url = new java.net.URL(endpoint);
            java.net.HttpURLConnection conn = (java.net.HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(8000);
            conn.setReadTimeout(8000);
            conn.setRequestProperty("Accept", "application/dns-json");

            int status = conn.getResponseCode();
            if (status != 200) {
                conn.disconnect();
                return sb.append("Failed to query DNS records. HTTP status: ").append(status).toString();
            }

            BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            StringBuilder jsonSb = new StringBuilder();
            String line;
            while ((line = br.readLine()) != null) {
                jsonSb.append(line);
            }
            br.close();
            conn.disconnect();

            return parseDnsJsonResponse(cleanHost, type, jsonSb.toString());
        } catch (Exception e) {
            return sb.append("Query error: ").append(e.getMessage()).toString();
        }
    }

    static String parseDnsJsonResponse(String cleanHost, String type, String jsonStr) {
        StringBuilder sb = new StringBuilder();
        sb.append("DNS Query Result for ").append(cleanHost).append(" [").append(type).append("]\n");
        sb.append("----------------------------------------------------\n");
        try {
            org.json.JSONObject obj = new org.json.JSONObject(jsonStr);
            int rcode = obj.optInt("Status", 0);
            String statusName = getDnsStatusName(rcode);
            sb.append("Status: ").append(statusName).append(" (Code ").append(rcode).append(")\n");
            sb.append("Truncated (TC): ").append(obj.optBoolean("TC", false)).append("\n");
            sb.append("Recursion Desired (RD): ").append(obj.optBoolean("RD", false)).append("\n");
            sb.append("Recursion Available (RA): ").append(obj.optBoolean("RA", false)).append("\n\n");

            if (obj.has("Answer")) {
                org.json.JSONArray answers = obj.getJSONArray("Answer");
                sb.append("Answers (").append(answers.length()).append("):\n");
                for (int i = 0; i < answers.length(); i++) {
                    org.json.JSONObject ans = answers.getJSONObject(i);
                    String ansName = ans.optString("name", "");
                    int ansType = ans.optInt("type", 0);
                    int ttl = ans.optInt("TTL", 0);
                    String data = ans.optString("data", "");
                    String typeName = getDnsTypeName(ansType);
                    sb.append("  • ").append(typeName).append("\tTTL: ").append(ttl).append("s\n")
                      .append("    Name: ").append(ansName).append("\n")
                      .append("    Data: ").append(data).append("\n\n");
                }
            } else if (obj.has("Authority")) {
                org.json.JSONArray auth = obj.getJSONArray("Authority");
                sb.append("Authority (").append(auth.length()).append("):\n");
                for (int i = 0; i < auth.length(); i++) {
                    org.json.JSONObject ans = auth.getJSONObject(i);
                    sb.append("  • ").append(ans.optString("name")).append(" => ").append(ans.optString("data")).append("\n");
                }
            } else {
                sb.append("No record answers found.\n");
            }

            if (obj.has("Comment")) {
                sb.append("\nServer Comment: ").append(obj.getString("Comment")).append("\n");
            }
        } catch (Exception e) {
            sb.append("Error parsing response: ").append(e.getMessage()).append("\nRaw response:\n").append(jsonStr);
        }
        return sb.toString();
    }

    private static String getDnsStatusName(int rcode) {
        switch (rcode) {
            case 0: return "NOERROR (Success)";
            case 1: return "FORMERR (Format Error)";
            case 2: return "SERVFAIL (Server Failure)";
            case 3: return "NXDOMAIN (Non-Existent Domain)";
            case 4: return "NOTIMP (Not Implemented)";
            case 5: return "REFUSED (Query Refused)";
            case 6: return "YXDOMAIN (Name Exists when it should not)";
            case 7: return "YXRRSET (RR Set Exists when it should not)";
            case 8: return "NXRRSET (RR Set that should exist does not)";
            case 9: return "NOTAUTH (Server Not Authoritative)";
            default: return "RCODE_" + rcode;
        }
    }

    private static String getDnsTypeName(int typeCode) {
        switch (typeCode) {
            case 1: return "A";
            case 2: return "NS";
            case 5: return "CNAME";
            case 6: return "SOA";
            case 12: return "PTR";
            case 15: return "MX";
            case 16: return "TXT";
            case 28: return "AAAA";
            case 33: return "SRV";
            case 257: return "CAA";
            default: return "TYPE_" + typeCode;
        }
    }

    // Built-in offline OUI prefix vendor dictionary for popular hardware vendors
    private static final java.util.Map<String, String> COMMON_OUIS = new java.util.HashMap<>();
    static {
        COMMON_OUIS.put("00:1A:2B", "Ayecom Technology Co., Ltd.");
        COMMON_OUIS.put("00:50:56", "VMware, Inc.");
        COMMON_OUIS.put("00:0C:29", "VMware, Inc.");
        COMMON_OUIS.put("00:05:69", "VMware, Inc.");
        COMMON_OUIS.put("08:00:27", "PCS Systemtechnik GmbH (Oracle VirtualBox)");
        COMMON_OUIS.put("52:54:00", "QEMU / KVM Virtual NIC");
        COMMON_OUIS.put("00:15:5D", "Microsoft Corporation (Hyper-V)");
        COMMON_OUIS.put("B8:27:EB", "Raspberry Pi Foundation");
        COMMON_OUIS.put("DC:A6:32", "Raspberry Pi Trading Ltd");
        COMMON_OUIS.put("E4:5F:01", "Raspberry Pi Trading Ltd");
        COMMON_OUIS.put("28:CD:C1", "Raspberry Pi Trading Ltd");
        COMMON_OUIS.put("2C:CF:67", "Raspberry Pi Trading Ltd");
        COMMON_OUIS.put("30:AE:A4", "Espressif Systems (Shanghai) Co., Ltd.");
        COMMON_OUIS.put("24:0A:C4", "Espressif Systems (Shanghai) Co., Ltd.");
        COMMON_OUIS.put("24:62:AB", "Espressif Systems (Shanghai) Co., Ltd.");
        COMMON_OUIS.put("84:0D:8E", "Espressif Systems (Shanghai) Co., Ltd.");
        COMMON_OUIS.put("A4:CF:12", "Espressif Systems (Shanghai) Co., Ltd.");
        COMMON_OUIS.put("3C:71:BF", "Espressif Systems (Shanghai) Co., Ltd.");
        COMMON_OUIS.put("00:1E:C2", "Apple, Inc.");
        COMMON_OUIS.put("00:25:00", "Apple, Inc.");
        COMMON_OUIS.put("3C:07:54", "Apple, Inc.");
        COMMON_OUIS.put("A4:83:E7", "Apple, Inc.");
        COMMON_OUIS.put("F0:18:98", "Apple, Inc.");
        COMMON_OUIS.put("AC:DE:48", "Apple, Inc.");
        COMMON_OUIS.put("F4:F5:D8", "Google LLC");
        COMMON_OUIS.put("3C:5A:B4", "Google LLC");
        COMMON_OUIS.put("D8:EB:97", "Google LLC");
        COMMON_OUIS.put("00:1A:11", "Google LLC");
        COMMON_OUIS.put("54:60:09", "Google LLC");
        COMMON_OUIS.put("00:04:96", "Extreme Networks");
        COMMON_OUIS.put("00:00:0C", "Cisco Systems, Inc");
        COMMON_OUIS.put("00:01:42", "Cisco Systems, Inc");
        COMMON_OUIS.put("00:01:C7", "Cisco Systems, Inc");
        COMMON_OUIS.put("00:01:C9", "Cisco Systems, Inc");
        COMMON_OUIS.put("00:08:E3", "Cisco Systems, Inc");
        COMMON_OUIS.put("00:14:1C", "Cisco Systems, Inc");
        COMMON_OUIS.put("00:1A:A1", "Cisco Systems, Inc");
        COMMON_OUIS.put("00:1A:A2", "Cisco Systems, Inc");
        COMMON_OUIS.put("00:26:99", "Cisco Systems, Inc");
        COMMON_OUIS.put("00:09:0F", "Fortinet, Inc.");
        COMMON_OUIS.put("70:4C:A5", "Fortinet, Inc.");
        COMMON_OUIS.put("00:13:10", "Cisco Linksys");
        COMMON_OUIS.put("00:14:BF", "Cisco Linksys");
        COMMON_OUIS.put("00:18:39", "Cisco Linksys");
        COMMON_OUIS.put("00:1B:2F", "Netgear");
        COMMON_OUIS.put("00:1E:2A", "Netgear");
        COMMON_OUIS.put("00:24:B2", "Netgear");
        COMMON_OUIS.put("00:26:F2", "Netgear");
        COMMON_OUIS.put("00:0F:B5", "Netgear");
        COMMON_OUIS.put("00:1D:7E", "Cisco Systems, Inc");
        COMMON_OUIS.put("00:17:88", "Philips Lighting BV (Hue)");
        COMMON_OUIS.put("EC:B5:FA", "Philips Lighting BV (Hue)");
        COMMON_OUIS.put("00:11:32", "Synology Incorporated");
        COMMON_OUIS.put("00:08:9B", "QNAP System Inc.");
        COMMON_OUIS.put("24:5E:BE", "QNAP System Inc.");
        COMMON_OUIS.put("00:1A:79", "Alcatel-Lucent IPD");
        COMMON_OUIS.put("00:14:4F", "Oracle Corporation");
        COMMON_OUIS.put("00:21:F6", "Intel Corporate");
        COMMON_OUIS.put("00:1B:21", "Intel Corporate");
        COMMON_OUIS.put("00:13:E8", "Intel Corporate");
        COMMON_OUIS.put("00:15:00", "Intel Corporate");
        COMMON_OUIS.put("00:16:76", "Intel Corporate");
        COMMON_OUIS.put("A4:4C:C8", "Dell Inc.");
        COMMON_OUIS.put("00:14:22", "Dell Inc.");
        COMMON_OUIS.put("18:66:DA", "Dell Inc.");
        COMMON_OUIS.put("B8:AC:6F", "Dell Inc.");
        COMMON_OUIS.put("00:1E:4F", "Dell Inc.");
        COMMON_OUIS.put("00:09:6B", "IBM Corporation");
        COMMON_OUIS.put("00:04:AC", "IBM Corporation");
        COMMON_OUIS.put("00:1A:64", "IBM Corporation");
        COMMON_OUIS.put("00:16:B6", "Sony Mobile Communications Inc");
        COMMON_OUIS.put("00:13:A9", "Sony Interactive Entertainment Inc.");
        COMMON_OUIS.put("00:15:C1", "Sony Interactive Entertainment Inc.");
        COMMON_OUIS.put("70:9E:29", "Sony Interactive Entertainment Inc.");
        COMMON_OUIS.put("F8:46:1C", "Sony Interactive Entertainment Inc.");
        COMMON_OUIS.put("00:04:4B", "NVIDIA Corporation");
        COMMON_OUIS.put("00:09:5B", "NETGEAR");
        COMMON_OUIS.put("00:0C:43", "Ralink Technology, Corp.");
        COMMON_OUIS.put("00:25:9C", "Cisco Systems, Inc");
        COMMON_OUIS.put("00:24:A5", "Buffalo Inc.");
        COMMON_OUIS.put("00:07:40", "Buffalo Inc.");
        COMMON_OUIS.put("00:21:29", "Cisco Systems, Inc");
        COMMON_OUIS.put("00:22:90", "Cisco Systems, Inc");
        COMMON_OUIS.put("00:16:B9", "Huawei Technologies Co., Ltd");
        COMMON_OUIS.put("00:18:82", "Huawei Technologies Co., Ltd");
        COMMON_OUIS.put("00:25:9E", "Huawei Technologies Co., Ltd");
        COMMON_OUIS.put("00:46:4B", "Huawei Device Co., Ltd.");
        COMMON_OUIS.put("00:E0:FC", "Huawei Technologies Co., Ltd");
        COMMON_OUIS.put("00:0C:E7", "Samsung Electronics Co.,Ltd");
        COMMON_OUIS.put("00:12:47", "Samsung Electronics Co.,Ltd");
        COMMON_OUIS.put("00:15:99", "Samsung Electronics Co.,Ltd");
        COMMON_OUIS.put("00:16:6C", "Samsung Electronics Co.,Ltd");
        COMMON_OUIS.put("00:1A:80", "Samsung Electronics Co.,Ltd");
        COMMON_OUIS.put("00:21:19", "Samsung Electronics Co.,Ltd");
        COMMON_OUIS.put("00:23:D7", "Samsung Electronics Co.,Ltd");
        COMMON_OUIS.put("00:24:54", "Samsung Electronics Co.,Ltd");
        COMMON_OUIS.put("00:26:37", "Samsung Electronics Co.,Ltd");
        COMMON_OUIS.put("00:1D:0F", "TP-LINK TECHNOLOGIES CO., LTD.");
        COMMON_OUIS.put("14:CF:92", "TP-Link Corporation Limited");
        COMMON_OUIS.put("18:D6:C7", "TP-LINK TECHNOLOGIES CO., LTD.");
        COMMON_OUIS.put("50:C7:BF", "TP-LINK TECHNOLOGIES CO., LTD.");
        COMMON_OUIS.put("54:AF:97", "TP-LINK TECHNOLOGIES CO., LTD.");
        COMMON_OUIS.put("C0:4A:00", "TP-LINK TECHNOLOGIES CO., LTD.");
        COMMON_OUIS.put("EC:08:6B", "TP-LINK TECHNOLOGIES CO., LTD.");
        COMMON_OUIS.put("00:18:0A", "Meraki, Inc.");
        COMMON_OUIS.put("00:22:6B", "Cisco-Linksys");
        COMMON_OUIS.put("44:D9:E7", "Ubiquiti Networks Inc.");
        COMMON_OUIS.put("80:2A:A8", "Ubiquiti Networks Inc.");
        COMMON_OUIS.put("B4:FB:E4", "Ubiquiti Networks Inc.");
        COMMON_OUIS.put("DC:9F:DB", "Ubiquiti Networks Inc.");
        COMMON_OUIS.put("F0:9F:C2", "Ubiquiti Networks Inc.");
        COMMON_OUIS.put("74:83:C2", "Ubiquiti Networks Inc.");
        COMMON_OUIS.put("68:D7:9A", "Ubiquiti Networks Inc.");
    }

    public static String extractOuiPrefix(String macOrOui) {
        if (macOrOui == null) return null;
        String clean = macOrOui.trim().toUpperCase().replaceAll("[^0-9A-F]", "");
        if (clean.length() < 6) return null;
        return clean.substring(0, 2) + ":" + clean.substring(2, 4) + ":" + clean.substring(4, 6);
    }

    public static String lookupMacVendor(String macOrOui) {
        if (macOrOui == null || macOrOui.trim().isEmpty()) {
            return "Please enter a MAC address or OUI prefix.";
        }
        String oui = extractOuiPrefix(macOrOui);
        if (oui == null) {
            return "Invalid input. Please provide at least 6 hexadecimal characters (e.g. 00:1A:2B or 001A2B).";
        }

        StringBuilder sb = new StringBuilder();
        sb.append("MAC / OUI Vendor Lookup\n");
        sb.append("====================================================\n");
        sb.append("Query:        ").append(macOrOui.trim()).append("\n");
        sb.append("OUI Prefix:   ").append(oui).append("\n\n");

        if (COMMON_OUIS.containsKey(oui)) {
            sb.append("Manufacturer: ").append(COMMON_OUIS.get(oui)).append("\n");
            sb.append("Source:       Built-in IEEE OUI Database\n");
            return sb.toString();
        }

        // Online lookup via macvendors API fallback
        try {
            java.net.URL url = new java.net.URL("https://api.macvendors.com/" + java.net.URLEncoder.encode(oui, "UTF-8"));
            java.net.HttpURLConnection conn = (java.net.HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(6000);
            conn.setReadTimeout(6000);
            conn.setRequestProperty("User-Agent", "Android NetworkTools/1.0");

            int status = conn.getResponseCode();
            if (status == 200) {
                BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                String vendor = br.readLine();
                br.close();
                conn.disconnect();
                sb.append("Manufacturer: ").append(vendor != null ? vendor.trim() : "Unknown").append("\n");
                sb.append("Source:       Online OUI Registry (macvendors.com)\n");
                return sb.toString();
            } else if (status == 404) {
                conn.disconnect();
                sb.append("Manufacturer: Vendor Not Found / Unregistered OUI\n");
                return sb.toString();
            } else {
                conn.disconnect();
                sb.append("Lookup returned HTTP status ").append(status).append("\n");
                return sb.toString();
            }
        } catch (Exception e) {
            sb.append("Online lookup failed: ").append(e.getMessage()).append("\n");
            sb.append("The OUI prefix was not found in the built-in offline database.\n");
            return sb.toString();
        }
    }

    public static String runSpeedTest(SpeedTestListener listener) {
        StringBuilder sb = new StringBuilder();
        sb.append("Network Speed & Latency Test\n");
        sb.append("====================================================\n\n");

        try {
            if (listener != null) listener.onProgress("Measuring Ping Latency & Jitter...");

            // 1. Latency & Jitter Measurement (10 pings against cloudflare speed test ping endpoint)
            long totalLatency = 0;
            long minLatency = Long.MAX_VALUE;
            long maxLatency = 0;
            List<Long> latencies = new ArrayList<>();
            String pingUrlStr = "https://speed.cloudflare.com/__down?bytes=0";

            for (int i = 0; i < 5; i++) {
                if (Thread.currentThread().isInterrupted()) {
                    return sb.append("Test cancelled.\n").toString();
                }
                long t0 = System.currentTimeMillis();
                java.net.URL url = new java.net.URL(pingUrlStr);
                java.net.HttpURLConnection conn = (java.net.HttpURLConnection) url.openConnection();
                conn.setConnectTimeout(5000);
                conn.setReadTimeout(5000);
                conn.setRequestProperty("User-Agent", "Mozilla/5.0 NetworkTools");
                conn.connect();
                conn.getResponseCode();
                conn.disconnect();
                long rtt = System.currentTimeMillis() - t0;
                latencies.add(rtt);
                totalLatency += rtt;
                if (rtt < minLatency) minLatency = rtt;
                if (rtt > maxLatency) maxLatency = rtt;
            }

            long avgLatency = totalLatency / latencies.size();
            long jitterSum = 0;
            for (int i = 1; i < latencies.size(); i++) {
                jitterSum += Math.abs(latencies.get(i) - latencies.get(i - 1));
            }
            long jitter = latencies.size() > 1 ? jitterSum / (latencies.size() - 1) : 0;

            sb.append("Ping Latency: ").append(avgLatency).append(" ms (Min: ").append(minLatency)
              .append(" ms, Max: ").append(maxLatency).append(" ms)\n");
            sb.append("Jitter:       ").append(jitter).append(" ms\n\n");

            // 2. Download speed test (download 5 MB stream from cloudflare)
            int downloadBytes = 5 * 1024 * 1024; // 5 MB
            if (listener != null) listener.onProgress("Testing Download Speed (5 MB payload)...");

            String dlUrlStr = "https://speed.cloudflare.com/__down?bytes=" + downloadBytes;
            java.net.URL dlUrl = new java.net.URL(dlUrlStr);
            java.net.HttpURLConnection dlConn = (java.net.HttpURLConnection) dlUrl.openConnection();
            dlConn.setConnectTimeout(8000);
            dlConn.setReadTimeout(15000);
            dlConn.setRequestProperty("User-Agent", "Mozilla/5.0 NetworkTools");

            long startDl = System.currentTimeMillis();
            dlConn.connect();
            java.io.InputStream is = dlConn.getInputStream();
            byte[] buf = new byte[8192];
            int read;
            long totalRead = 0;
            while ((read = is.read(buf)) != -1) {
                totalRead += read;
                if (Thread.currentThread().isInterrupted()) {
                    is.close();
                    dlConn.disconnect();
                    return sb.append("Test cancelled.\n").toString();
                }
            }
            is.close();
            dlConn.disconnect();
            long dlDurationMs = Math.max(1, System.currentTimeMillis() - startDl);

            double durationSec = dlDurationMs / 1000.0;
            double megabits = (totalRead * 8.0) / (1024.0 * 1024.0);
            double mbps = megabits / durationSec;

            sb.append("Download Size: ").append(String.format(java.util.Locale.US, "%.2f MB", totalRead / (1024.0 * 1024.0))).append("\n");
            sb.append("Time Taken:    ").append(String.format(java.util.Locale.US, "%.2f seconds", durationSec)).append("\n");
            sb.append("Download Speed:").append(String.format(java.util.Locale.US, " %.2f Mbps", mbps)).append("\n\n");
            sb.append("Speed test complete.\n");

            return sb.toString();
        } catch (Exception e) {
            return sb.append("Speed test failed: ").append(e.getMessage()).append("\n").toString();
        }
    }

    public static String discoverSsdpDevices(int timeoutMs) {
        if (timeoutMs <= 0) {
            timeoutMs = 3000;
        }

        StringBuilder sb = new StringBuilder();
        sb.append("SSDP / UPnP Device Discovery\n");
        sb.append("====================================================\n");
        sb.append("Multicast Group: 239.255.255.250:1900\n");
        sb.append("Listening for up to ").append(timeoutMs / 1000.0).append("s...\n\n");

        String mSearch = "M-SEARCH * HTTP/1.1\r\n" +
                "HOST: 239.255.255.250:1900\r\n" +
                "MAN: \"ssdp:discover\"\r\n" +
                "MX: " + Math.max(1, timeoutMs / 1000) + "\r\n" +
                "ST: ssdp:all\r\n\r\n";

        byte[] sendData = mSearch.getBytes(java.nio.charset.StandardCharsets.UTF_8);

        java.net.DatagramSocket socket = null;
        int deviceCount = 0;
        java.util.Set<String> seenLocations = new java.util.HashSet<>();

        try {
            socket = new java.net.DatagramSocket();
            socket.setSoTimeout(1000);
            socket.setBroadcast(true);

            java.net.InetAddress group = java.net.InetAddress.getByName("239.255.255.250");
            java.net.DatagramPacket sendPacket = new java.net.DatagramPacket(sendData, sendData.length, group, 1900);
            socket.send(sendPacket);

            long endTime = System.currentTimeMillis() + timeoutMs;
            byte[] recvBuf = new byte[4096];

            while (System.currentTimeMillis() < endTime && !Thread.currentThread().isInterrupted()) {
                java.net.DatagramPacket recvPacket = new java.net.DatagramPacket(recvBuf, recvBuf.length);
                try {
                    socket.receive(recvPacket);
                    String resp = new String(recvPacket.getData(), 0, recvPacket.getLength(), java.nio.charset.StandardCharsets.UTF_8);
                    String senderIp = recvPacket.getAddress().getHostAddress();

                    String location = extractSsdpHeader(resp, "LOCATION");
                    String server = extractSsdpHeader(resp, "SERVER");
                    String st = extractSsdpHeader(resp, "ST");
                    String usn = extractSsdpHeader(resp, "USN");

                    String key = location != null ? location : (senderIp + "_" + usn);
                    if (!seenLocations.contains(key)) {
                        seenLocations.add(key);
                        deviceCount++;
                        sb.append("[").append(deviceCount).append("] Device: ").append(senderIp).append("\n");
                        if (server != null && !server.isEmpty()) sb.append("    Server:   ").append(server).append("\n");
                        if (location != null && !location.isEmpty()) sb.append("    Location: ").append(location).append("\n");
                        if (st != null && !st.isEmpty()) sb.append("    Type:     ").append(st).append("\n");
                        if (usn != null && !usn.isEmpty()) sb.append("    USN:      ").append(usn).append("\n");
                        sb.append("\n");
                    }
                } catch (java.net.SocketTimeoutException ignored) {
                    // socket read timeout, continue loop until endTime
                }
            }

            if (deviceCount == 0) {
                sb.append("No SSDP / UPnP devices responded within timeout.\n")
                  .append("Note: Device discovery requires being connected to a local Wi-Fi network that allows UDP multicast.\n");
            } else {
                sb.append("Discovered ").append(deviceCount).append(" unique device(s) or service(s).\n");
            }
        } catch (Exception e) {
            sb.append("Discovery error: ").append(e.getMessage()).append("\n");
        } finally {
            if (socket != null && !socket.isClosed()) {
                socket.close();
            }
        }

        return sb.toString();
    }

    static String extractSsdpHeader(String response, String headerName) {
        if (response == null || headerName == null) return null;
        String[] lines = response.split("\r?\n");
        String prefix = headerName.toLowerCase() + ":";
        for (String line : lines) {
            if (line.toLowerCase().startsWith(prefix)) {
                return line.substring(prefix.length()).trim();
            }
        }
        return null;
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
