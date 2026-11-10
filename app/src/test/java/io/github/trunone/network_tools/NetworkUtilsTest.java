package io.github.trunone.network_tools;

import org.junit.Test;
import static org.junit.Assert.*;

public class NetworkUtilsTest {

    @Test
    public void testPingValidation() {
        assertEquals("Please enter a host.", NetworkUtils.ping(null));
        assertEquals("Please enter a host.", NetworkUtils.ping(""));
        assertEquals("Please enter a host.", NetworkUtils.ping("   "));
    }

    @Test
    public void testTracerouteValidation() {
        assertEquals("Please enter a host.", NetworkUtils.traceroute(null));
        assertEquals("Please enter a host.", NetworkUtils.traceroute(""));
        assertEquals("Please enter a host.", NetworkUtils.traceroute("   "));
    }

    @Test
    public void testInvalidHost() {
        String expectedError = "Invalid host. Only alphanumeric characters, dots, hyphens, and colons are allowed.";
        assertEquals(expectedError, NetworkUtils.ping("google.com; ls"));
        assertEquals(expectedError, NetworkUtils.traceroute("google.com && echo 1"));
    }

    @Test
    public void testParseIpFromPingOutputIpv4() {
        String output1 = "64 bytes from 192.168.1.1: icmp_seq=1 ttl=64 time=0.04 ms";
        assertEquals("192.168.1.1", NetworkUtils.parseIpFromPingOutput(output1));

        String output2 = "From 10.0.0.1 icmp_seq=1 Time to live exceeded";
        assertEquals("10.0.0.1", NetworkUtils.parseIpFromPingOutput(output2));

        String output3 = "64 bytes from router.local (192.168.1.254): icmp_seq=1 ttl=64 time=1.2 ms";
        assertEquals("192.168.1.254", NetworkUtils.parseIpFromPingOutput(output3));
    }

    @Test
    public void testParseIpFromPingOutputIpv6() {
        String output1 = "64 bytes from 2001:db8::1: icmp_seq=1 ttl=64 time=0.04 ms";
        assertEquals("2001:db8::1", NetworkUtils.parseIpFromPingOutput(output1));

        String output2 = "From 2001:db8::1 icmp_seq=1 Time to live exceeded";
        assertEquals("2001:db8::1", NetworkUtils.parseIpFromPingOutput(output2));
    }

    @Test
    public void testDnsLookupValidation() {
        assertEquals("Please enter a host.", NetworkUtils.dnsLookup(null));
        assertEquals("Please enter a host.", NetworkUtils.dnsLookup(""));
        assertEquals("Please enter a host.", NetworkUtils.dnsLookup("   "));
        assertTrue(NetworkUtils.dnsLookup("invalid host; ls").contains("Invalid host"));
    }

    @Test
    public void testDnsLookupLoopback() {
        String result = NetworkUtils.dnsLookup("localhost");
        assertTrue(result.contains("DNS Lookup for localhost:"));
        assertTrue(result.contains("127.0.0.1") || result.contains("::1"));
    }

    @Test
    public void testPortScannerValidation() {
        assertEquals("Please enter a host.", NetworkUtils.scanPorts(null, 80, 80, 100));
        assertEquals("Please enter a host.", NetworkUtils.scanPorts("", 80, 80, 100));
        assertTrue(NetworkUtils.scanPorts("invalid host; ls", 80, 80, 100).contains("Invalid host"));

        assertTrue(NetworkUtils.scanPorts("127.0.0.1", -1, 80, 100).contains("Invalid port range"));
        assertTrue(NetworkUtils.scanPorts("127.0.0.1", 100, 80, 100).contains("Invalid port range"));
        assertTrue(NetworkUtils.scanPorts("127.0.0.1", 1, 70000, 100).contains("Invalid port range"));
        assertTrue(NetworkUtils.scanPorts("127.0.0.1", 1, 1500, 100).contains("Port range too large"));
    }

    @Test
    public void testCalculateSubnetValidation() {
        assertEquals("Please enter an IP address.", NetworkUtils.calculateSubnet(null, 24));
        assertEquals("Please enter an IP address.", NetworkUtils.calculateSubnet("", 24));
        assertTrue(NetworkUtils.calculateSubnet("192.168.1.1", -1).contains("Invalid prefix length"));
        assertTrue(NetworkUtils.calculateSubnet("192.168.1.1", 33).contains("Invalid prefix length"));
        assertTrue(NetworkUtils.calculateSubnet("192.168.1", 24).contains("Invalid IPv4 address format"));
        assertTrue(NetworkUtils.calculateSubnet("192.168.1.300", 24).contains("Invalid octet"));
    }

    @Test
    public void testCalculateSubnetClassC() {
        String result = NetworkUtils.calculateSubnet("192.168.1.50", 24);
        assertTrue(result.contains("Subnet Mask: 255.255.255.0"));
        assertTrue(result.contains("Wildcard Mask: 0.0.0.255"));
        assertTrue(result.contains("Network Address: 192.168.1.0"));
        assertTrue(result.contains("Broadcast Address: 192.168.1.255"));
        assertTrue(result.contains("Usable Host Range: 192.168.1.1 - 192.168.1.254"));
        assertTrue(result.contains("Total Hosts: 256"));
        assertTrue(result.contains("Usable Hosts: 254"));
    }

    @Test
    public void testCalculateSubnetSlash30() {
        String result = NetworkUtils.calculateSubnet("10.0.0.5", 30);
        assertTrue(result.contains("Subnet Mask: 255.255.255.252"));
        assertTrue(result.contains("Network Address: 10.0.0.4"));
        assertTrue(result.contains("Broadcast Address: 10.0.0.7"));
        assertTrue(result.contains("Usable Host Range: 10.0.0.5 - 10.0.0.6"));
        assertTrue(result.contains("Total Hosts: 4"));
        assertTrue(result.contains("Usable Hosts: 2"));
    }

    @Test
    public void testCalculateSubnetSlash32() {
        String result = NetworkUtils.calculateSubnet("10.0.0.1", 32);
        assertTrue(result.contains("Subnet Mask: 255.255.255.255"));
        assertTrue(result.contains("Network Address: 10.0.0.1"));
        assertTrue(result.contains("Broadcast Address: 10.0.0.1"));
        assertTrue(result.contains("Total Hosts: 1"));
        assertTrue(result.contains("Usable Hosts: 1"));
    }

    @Test
    public void testParseMac() {
        assertNull(NetworkUtils.parseMac(null));
        assertNull(NetworkUtils.parseMac(""));
        assertNull(NetworkUtils.parseMac("00:11:22:33:44"));
        assertNull(NetworkUtils.parseMac("00:11:22:33:44:55:66"));
        assertNull(NetworkUtils.parseMac("00:11:22:33:44:ZZ"));

        byte[] macColon = NetworkUtils.parseMac("00:1A:2B:3C:4D:5E");
        assertNotNull(macColon);
        assertEquals(6, macColon.length);
        assertEquals((byte) 0x00, macColon[0]);
        assertEquals((byte) 0x1A, macColon[1]);
        assertEquals((byte) 0x5E, macColon[5]);

        byte[] macDash = NetworkUtils.parseMac("00-1a-2b-3c-4d-5e");
        assertNotNull(macDash);
        assertArrayEquals(macColon, macDash);
    }

    @Test
    public void testWakeOnLanValidation() {
        assertEquals("Please enter a MAC address.", NetworkUtils.sendWakeOnLan(null, "255.255.255.255", 9));
        assertEquals("Please enter a MAC address.", NetworkUtils.sendWakeOnLan("", "255.255.255.255", 9));
        assertTrue(NetworkUtils.sendWakeOnLan("invalid-mac", "255.255.255.255", 9).contains("Invalid MAC address"));
    }

    @Test
    public void testIpGeoValidation() {
        assertTrue(NetworkUtils.getIpGeoInfo("invalid host; ls").contains("Invalid host or IP"));
    }

    @Test
    public void testFormatIpGeoJson() {
        String sampleJson = "{" +
                "\"ip\":\"8.8.8.8\"," +
                "\"success\":true," +
                "\"type\":\"IPv4\"," +
                "\"continent\":\"North America\"," +
                "\"continent_code\":\"NA\"," +
                "\"country\":\"United States\"," +
                "\"country_code\":\"US\"," +
                "\"region\":\"California\"," +
                "\"city\":\"San Jose\"," +
                "\"postal\":\"95025\"," +
                "\"latitude\":37.339," +
                "\"longitude\":-121.894," +
                "\"connection\":{\"asn\":15169,\"isp\":\"Google LLC\",\"org\":\"Google LLC\"}," +
                "\"timezone\":{\"id\":\"America/Los_Angeles\",\"utc\":\"-08:00\"}" +
                "}";
        String formatted = NetworkUtils.formatIpGeoJson(sampleJson);
        assertTrue(formatted.contains("IP Address:   8.8.8.8"));
        assertTrue(formatted.contains("Country:      United States (US)"));
        assertTrue(formatted.contains("City:         San Jose"));
        assertTrue(formatted.contains("ASN:          AS15169"));
        assertTrue(formatted.contains("ISP:          Google LLC"));
    }

    @Test
    public void testFormatIpGeoJsonError() {
        String errorJson = "{\"success\":false,\"message\":\"Reserved IP address\"}";
        String formatted = NetworkUtils.formatIpGeoJson(errorJson);
        assertTrue(formatted.contains("Error: Reserved IP address"));
    }

    @Test
    public void testWhoisValidation() {
        assertEquals("Please enter a domain or IP address.", NetworkUtils.whois(null));
        assertEquals("Please enter a domain or IP address.", NetworkUtils.whois(""));
        assertEquals("Please enter a domain or IP address.", NetworkUtils.whois("   "));
        assertTrue(NetworkUtils.whois("example.com; rm -rf").contains("Invalid host or IP"));
    }

    @Test
    public void testExtractReferralServer() {
        String whoisOutput = "domain: EXAMPLE.COM\nwhois: whois.iana.org\nstatus: active";
        assertEquals("whois.iana.org", NetworkUtils.extractReferralServer(whoisOutput));

        String whoisRefer = "domain: EXAMPLE.COM\nrefer: whois.verisign-grs.com\n";
        assertEquals("whois.verisign-grs.com", NetworkUtils.extractReferralServer(whoisRefer));

        String noRefer = "domain: EXAMPLE.COM\nstatus: active";
        assertNull(NetworkUtils.extractReferralServer(noRefer));
    }

    @Test
    public void testArpTableExecution() {
        String result = NetworkUtils.getArpTable();
        assertNotNull(result);
        assertTrue(result.contains("NEIGHBOR / ARP CACHE"));
        assertTrue(result.contains("ACTIVE LOCAL NEIGHBORS"));
    }

    @Test
    public void testArpTableWithContextNull() {
        String result = NetworkUtils.getArpTable(null);
        assertNotNull(result);
        assertTrue(result.contains("NEIGHBOR / ARP CACHE"));
    }

    @Test
    public void testHttpInspectValidation() {
        assertEquals("Please enter a URL or host.", NetworkUtils.httpInspect(null));
        assertEquals("Please enter a URL or host.", NetworkUtils.httpInspect(""));
        assertEquals("Please enter a URL or host.", NetworkUtils.httpInspect("   "));
    }

    @Test
    public void testHttpInspectExecution() throws Exception {
        java.net.ServerSocket serverSocket = new java.net.ServerSocket(0);
        int port = serverSocket.getLocalPort();
        Thread serverThread = new Thread(() -> {
            try (java.net.Socket client = serverSocket.accept()) {
                java.io.BufferedReader in = new java.io.BufferedReader(new java.io.InputStreamReader(client.getInputStream()));
                String line;
                while ((line = in.readLine()) != null && !line.isEmpty()) {
                    // read request headers
                }
                java.io.OutputStream out = client.getOutputStream();
                String response = "HTTP/1.1 200 OK\r\nX-Test-Header: NetworkToolsRule\r\nContent-Length: 2\r\n\r\nOK";
                out.write(response.getBytes());
                out.flush();
            } catch (Exception ignored) {
            }
        });
        serverThread.start();
        try {
            String result = NetworkUtils.httpInspect("http://127.0.0.1:" + port + "/test");
            assertTrue(result.contains("Response: 200"));
            assertTrue(result.contains("X-Test-Header: NetworkToolsRule"));
        } finally {
            serverSocket.close();
            serverThread.join(2000);
        }
    }

    @Test
    public void testSslCertValidation() {
        assertEquals("Please enter a host.", NetworkUtils.getSslCertificateInfo(null, 443, 1000));
        assertEquals("Please enter a host.", NetworkUtils.getSslCertificateInfo("", 443, 1000));
        assertEquals("Please enter a host.", NetworkUtils.getSslCertificateInfo("   ", 443, 1000));
        assertTrue(NetworkUtils.getSslCertificateInfo("bad host; ls", 443, 1000).contains("Invalid host"));
    }

    @Test
    public void testDnsRecordsValidation() {
        assertEquals("Please enter a host or domain name.", NetworkUtils.queryDnsRecords(null, "A"));
        assertEquals("Please enter a host or domain name.", NetworkUtils.queryDnsRecords("", "A"));
        assertTrue(NetworkUtils.queryDnsRecords("bad host; ls", "A").contains("Invalid host"));
    }

    @Test
    public void testParseDnsJsonResponse() {
        String json = "{" +
                "\"Status\":0," +
                "\"TC\":false," +
                "\"RD\":true," +
                "\"RA\":true," +
                "\"Question\":[{\"name\":\"google.com.\",\"type\":1}]," +
                "\"Answer\":[{\"name\":\"google.com.\",\"type\":1,\"TTL\":300,\"data\":\"142.250.190.46\"}]" +
                "}";
        String formatted = NetworkUtils.parseDnsJsonResponse("google.com", "A", json);
        assertTrue(formatted.contains("Status: NOERROR (Success)"));
        assertTrue(formatted.contains("Answers (1):"));
        assertTrue(formatted.contains("• A"));
        assertTrue(formatted.contains("TTL: 300s"));
        assertTrue(formatted.contains("Data: 142.250.190.46"));
    }

    @Test
    public void testExtractOuiPrefix() {
        assertNull(NetworkUtils.extractOuiPrefix(null));
        assertNull(NetworkUtils.extractOuiPrefix(""));
        assertNull(NetworkUtils.extractOuiPrefix("12:34"));
        assertEquals("00:1A:2B", NetworkUtils.extractOuiPrefix("00:1a:2b:3c:4d:5e"));
        assertEquals("B8:27:EB", NetworkUtils.extractOuiPrefix("b8-27-eb-11-22-33"));
        assertEquals("00:15:5D", NetworkUtils.extractOuiPrefix("00155D123456"));
    }

    @Test
    public void testLookupMacVendorBuiltIn() {
        String result = NetworkUtils.lookupMacVendor("B8:27:EB:AA:BB:CC");
        assertTrue(result.contains("Raspberry Pi Foundation"));
        assertTrue(result.contains("Built-in IEEE OUI Database"));

        String appleResult = NetworkUtils.lookupMacVendor("00-1E-C2-00-11-22");
        assertTrue(appleResult.contains("Apple, Inc."));

        String espressifResult = NetworkUtils.lookupMacVendor("30AEA4123456");
        assertTrue(espressifResult.contains("Espressif Systems"));
    }

    @Test
    public void testLookupMacVendorInvalid() {
        assertEquals("Please enter a MAC address or OUI prefix.", NetworkUtils.lookupMacVendor(null));
        assertEquals("Please enter a MAC address or OUI prefix.", NetworkUtils.lookupMacVendor(""));
        assertTrue(NetworkUtils.lookupMacVendor("12:34").contains("Invalid input"));
    }

    @Test
    public void testExtractSsdpHeader() {
        String resp = "HTTP/1.1 200 OK\r\n" +
                "LOCATION: http://192.168.1.1:49152/description.xml\r\n" +
                "SERVER: Linux/2.6.30 UPnP/1.0 Sonos/56.0\r\n" +
                "ST: urn:schemas-upnp-org:device:ZonePlayer:1\r\n\r\n";

        assertEquals("http://192.168.1.1:49152/description.xml", NetworkUtils.extractSsdpHeader(resp, "LOCATION"));
        assertEquals("Linux/2.6.30 UPnP/1.0 Sonos/56.0", NetworkUtils.extractSsdpHeader(resp, "SERVER"));
        assertEquals("urn:schemas-upnp-org:device:ZonePlayer:1", NetworkUtils.extractSsdpHeader(resp, "ST"));
        assertNull(NetworkUtils.extractSsdpHeader(resp, "NON_EXISTENT_HEADER"));
    }
}
