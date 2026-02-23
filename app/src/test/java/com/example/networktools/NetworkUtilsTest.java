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
    public void testPingParameterizedValidation() {
        assertEquals("Please enter a host.", NetworkUtils.ping(null, 4, 1));
        assertEquals("Please enter a host.", NetworkUtils.ping("", 4, 1));
        String expectedError = "Invalid host. Only alphanumeric characters, dots, hyphens, and colons are allowed.";
        assertEquals(expectedError, NetworkUtils.ping("google.com; ls", 4, 1));
    }

    @Test
    public void testTracerouteParameterizedValidation() {
        assertEquals("Please enter a host.", NetworkUtils.traceroute(null, 30, 1));
        assertEquals("Please enter a host.", NetworkUtils.traceroute("", 30, 1));
        String expectedError = "Invalid host. Only alphanumeric characters, dots, hyphens, and colons are allowed.";
        assertEquals(expectedError, NetworkUtils.traceroute("google.com && echo 1", 30, 1));
    }
}
