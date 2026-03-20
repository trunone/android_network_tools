package io.github.trunone.network_tools;

import org.junit.Test;
import static org.junit.Assert.*;

public class NetworkUtilsOverloadTest {

    @Test
    public void testPingOverloadValidation() {
        assertEquals("Please enter a host.", NetworkUtils.ping(null, 4, 1));
        assertEquals("Please enter a host.", NetworkUtils.ping("", 4, 1));
    }

    @Test
    public void testTracerouteOverloadValidation() {
        assertEquals("Please enter a host.", NetworkUtils.traceroute(null, 30, 1));
        assertEquals("Please enter a host.", NetworkUtils.traceroute("", 30, 1));
    }
}
