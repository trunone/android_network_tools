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
}
