package com.example.networktools;

import org.junit.Test;
import static org.junit.Assert.*;

public class LocalNetworkTest {

    @Test
    public void testGetLocalNetworkInfo() {
        String info = NetworkUtils.getLocalNetworkInfo();
        assertNotNull(info);
        // We expect at least some output, even if it's just the error message or empty "Routes" header
        assertTrue(info.length() > 0);

        // Check if it contains expected headers
        // Note: In a test environment without networking, it might not find interfaces,
        // but the method should not crash and should return a string.
        // It should definitely contain the "Routes (Gateway info):" part which is appended at the end.
        assertTrue(info.contains("Routes (Gateway info):"));
    }
}
