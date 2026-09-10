package in.proofofconcept.url.shortner.service;

import in.proofofconcept.url.shortner.exception.CustomException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class SafeBrowsingServiceTest {

    private SafeBrowsingService safeBrowsingService;

    @BeforeEach
    void setUp() {
        // Disabled API key for local test suite
        safeBrowsingService = new SafeBrowsingService("", false);
    }

    @Test
    void testSafeUrlsPassValidation() {
        assertTrue(safeBrowsingService.isUrlSafe("https://github.com/spring-projects/spring-boot"));
        assertTrue(safeBrowsingService.isUrlSafe("https://docs.oracle.com/en/java/"));
        assertDoesNotThrow(() -> safeBrowsingService.validateUrlSafety("https://google.com"));
    }

    @Test
    void testKnownMaliciousDomainsAreFlagged() {
        assertFalse(safeBrowsingService.isUrlSafe("http://malware.testing.google.test/index.html"));
        assertFalse(safeBrowsingService.isUrlSafe("http://testsafebrowsing.appspot.com/s/phishing.html"));
        assertFalse(safeBrowsingService.isUrlSafe("https://badsite.test/login"));

        assertThrows(CustomException.class, () ->
                safeBrowsingService.validateUrlSafety("http://malware.testing.google.test"));
    }

    @Test
    void testSuspiciousRawIpHostFlagged() {
        assertFalse(safeBrowsingService.isUrlSafe("http://45.33.32.156/malicious-payload"));
    }
}
