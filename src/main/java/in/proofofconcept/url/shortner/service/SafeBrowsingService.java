package in.proofofconcept.url.shortner.service;

import in.proofofconcept.url.shortner.exception.CustomException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.net.URI;
import java.util.*;
import java.util.regex.Pattern;

@Service
@Slf4j
public class SafeBrowsingService {

    private static final String GOOGLE_SAFE_BROWSING_URL =
            "https://safebrowsing.googleapis.com/v4/threatMatches:find?key=";

    // Common test & known malicious domains/hosts for local threat simulation
    private static final Set<String> LOCAL_MALICIOUS_DOMAINS = Set.of(
            "testsafebrowsing.appspot.com",
            "malware.testing.google.test",
            "phishing.example.com",
            "malware.example.com",
            "badsite.test",
            "phishing-test.com"
    );

    // IP address host pattern (e.g., http://192.168.1.1 or http://45.33.32.156) often used in phishing
    private static final Pattern IP_HOST_PATTERN =
            Pattern.compile("^(\\d{1,3}\\.){3}\\d{1,3}$");

    private final String apiKey;
    private final boolean enabled;
    private final RestTemplate restTemplate;

    public SafeBrowsingService(
            @Value("${google.safebrowsing.api-key:}") String apiKey,
            @Value("${google.safebrowsing.enabled:false}") boolean enabled) {
        this.apiKey = apiKey;
        this.enabled = enabled && apiKey != null && !apiKey.isBlank();
        this.restTemplate = new RestTemplate();
    }

    /**
     * Scans a target URL against Threat Intelligence (Google Safe Browsing API + Local Heuristics).
     * @return true if safe, false if malicious
     */
    public boolean isUrlSafe(String url) {
        if (url == null || url.isBlank()) {
            return false;
        }

        // 1. Local Heuristic & Blacklist Checks
        if (!checkLocalThreats(url)) {
            log.warn("URL flagged as unsafe by local threat heuristics: {}", url);
            return false;
        }

        // 2. Google Safe Browsing API check if enabled and configured
        if (enabled) {
            try {
                boolean safeBrowsingResult = checkGoogleSafeBrowsing(url);
                if (!safeBrowsingResult) {
                    log.warn("URL flagged as malicious by Google Safe Browsing API: {}", url);
                    return false;
                }
            } catch (Exception e) {
                log.error("Google Safe Browsing API lookup failed, defaulting to local heuristics: {}", e.getMessage());
            }
        }

        return true;
    }

    /**
     * Verifies that the URL is safe, or throws CustomException.
     */
    public void validateUrlSafety(String url) {
        if (!isUrlSafe(url)) {
            throw new CustomException("Security Alert: Destination URL is flagged as unsafe, phishing, or malicious");
        }
    }

    private boolean checkLocalThreats(String url) {
        try {
            URI uri = URI.create(url);
            String host = uri.getHost();
            if (host == null) {
                return false;
            }

            String lowerHost = host.toLowerCase();

            // Check against known malicious domain list
            if (LOCAL_MALICIOUS_DOMAINS.contains(lowerHost)) {
                return false;
            }

            for (String maliciousDomain : LOCAL_MALICIOUS_DOMAINS) {
                if (lowerHost.endsWith("." + maliciousDomain)) {
                    return false;
                }
            }

            // Detect raw IP hosts without proper hostname
            if (IP_HOST_PATTERN.matcher(lowerHost).matches()) {
                // Allow localhost in development, but flag public raw IPs
                if (!lowerHost.equals("127.0.0.1")) {
                    log.warn("Flagged suspicious raw IP address host: {}", lowerHost);
                    return false;
                }
            }

            // Detect suspicious keywords in URL path or query
            String lowerUrl = url.toLowerCase();
            if (lowerUrl.contains("bank-login-verify") || lowerUrl.contains("free-crypto-giveaway")
                    || lowerUrl.contains("account-verification-update-security")) {
                return false;
            }

            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private boolean checkGoogleSafeBrowsing(String url) {
        String endpoint = GOOGLE_SAFE_BROWSING_URL + apiKey;

        Map<String, Object> requestBody = new HashMap<>();
        Map<String, String> client = new HashMap<>();
        client.put("clientId", "url-shortener-backend");
        client.put("clientVersion", "1.0.0");
        requestBody.put("client", client);

        Map<String, Object> threatInfo = new HashMap<>();
        threatInfo.put("threatTypes", List.of("MALWARE", "SOCIAL_ENGINEERING", "UNWANTED_SOFTWARE", "POTENTIALLY_HARMFUL_APPLICATION"));
        threatInfo.put("platformTypes", List.of("ANY_PLATFORM"));
        threatInfo.put("threatEntryTypes", List.of("URL"));
        threatInfo.put("threatEntries", List.of(Map.of("url", url)));
        requestBody.put("threatInfo", threatInfo);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

        ResponseEntity<Map> response = restTemplate.postForEntity(endpoint, entity, Map.class);
        if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
            // If threatMatches exists in the response, the URL is malicious
            return !response.getBody().containsKey("matches");
        }

        return true;
    }
}
