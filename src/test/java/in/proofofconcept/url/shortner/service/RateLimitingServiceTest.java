package in.proofofconcept.url.shortner.service;

import io.github.bucket4j.ConsumptionProbe;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class RateLimitingServiceTest {

    private RateLimitingService rateLimitingService;

    @BeforeEach
    void setUp() {
        rateLimitingService = new RateLimitingService();
    }

    @Test
    void testRateLimiterAllowsInitialRequests() {
        ConsumptionProbe probe = rateLimitingService.tryConsumeRedirect("192.168.1.100");
        assertTrue(probe.isConsumed());
        assertTrue(probe.getRemainingTokens() >= 0);
    }

    @Test
    void testAuthRateLimiterExhaustion() {
        String testIp = "10.0.0.50";
        // Auth limit is 10 requests
        for (int i = 0; i < 10; i++) {
            ConsumptionProbe probe = rateLimitingService.tryConsumeAuth(testIp);
            assertTrue(probe.isConsumed(), "Request " + (i + 1) + " should be consumed");
        }

        // 11th request must fail
        ConsumptionProbe deniedProbe = rateLimitingService.tryConsumeAuth(testIp);
        assertFalse(deniedProbe.isConsumed(), "11th request must exceed rate limit");
    }

    @Test
    void testDistinctIpIsolation() {
        String ip1 = "172.16.0.1";
        String ip2 = "172.16.0.2";

        // Exhaust IP1 auth bucket
        for (int i = 0; i < 10; i++) {
            rateLimitingService.tryConsumeAuth(ip1);
        }
        assertFalse(rateLimitingService.tryConsumeAuth(ip1).isConsumed());

        // IP2 should still be allowed
        assertTrue(rateLimitingService.tryConsumeAuth(ip2).isConsumed());
    }
}
