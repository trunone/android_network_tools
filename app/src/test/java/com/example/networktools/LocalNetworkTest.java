package com.example.networktools;

import org.junit.Test;
import static org.junit.Assert.*;

public class LocalNetworkTest {

    @Test
    public void testGetLocalNetworkInfo() {
        // Pass null as context for unit testing, it will attempt to use 'ip route' or just return empty if it fails.
        String info = NetworkUtils.getLocalNetworkInfo(null);
        assertNotNull(info);
        assertTrue(info.length() > 0);

        // Check for the new header
        assertTrue(info.contains("Gateway Information:"));
    }
}
