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
}
