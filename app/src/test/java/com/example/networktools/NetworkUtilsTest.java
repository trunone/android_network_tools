package com.example.networktools;

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
}
