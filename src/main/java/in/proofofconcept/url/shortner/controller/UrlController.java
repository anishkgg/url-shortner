package in.proofofconcept.url.shortner.controller;

import in.proofofconcept.url.shortner.dto.event.ClickEvent;
import in.proofofconcept.url.shortner.dto.request.UrlRequest;
import in.proofofconcept.url.shortner.dto.response.ClickAnalyticsResponse;
import in.proofofconcept.url.shortner.dto.response.UrlResponse;
import in.proofofconcept.url.shortner.exception.CustomException;
import in.proofofconcept.url.shortner.model.RedirectType;
import in.proofofconcept.url.shortner.model.Url;
import in.proofofconcept.url.shortner.model.User;
import in.proofofconcept.url.shortner.service.AnalyticsService;
import in.proofofconcept.url.shortner.service.QrCodeService;
import in.proofofconcept.url.shortner.service.RateLimitingService;
import in.proofofconcept.url.shortner.service.UrlService;
import io.github.bucket4j.ConsumptionProbe;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;
import java.util.Map;

@RestController
public class UrlController {

    private final UrlService urlService;
    private final AnalyticsService analyticsService;
    private final RateLimitingService rateLimitingService;
    private final QrCodeService qrCodeService;

    public UrlController(
            UrlService urlService,
            AnalyticsService analyticsService,
            RateLimitingService rateLimitingService,
            QrCodeService qrCodeService) {
        this.urlService = urlService;
        this.analyticsService = analyticsService;
        this.rateLimitingService = rateLimitingService;
        this.qrCodeService = qrCodeService;
    }

    /**
     * High-speed public redirection endpoint supporting both /r/{shortUrl} and /api/v1/url/{shortUrl}
     */
    @GetMapping({"/r/{shortUrl}", "/api/v1/url/{shortUrl}"})
    public ResponseEntity<?> redirectUrl(
            @PathVariable String shortUrl,
            @RequestHeader(value = "X-Password", required = false) String headerPassword,
            @RequestParam(value = "password", required = false) String queryPassword,
            HttpServletRequest request) {

        String clientIp = request.getRemoteAddr();

        // 1. Layer 7 Rate Limiting
        ConsumptionProbe probe = rateLimitingService.tryConsumeRedirect(clientIp);
        if (!probe.isConsumed()) {
            long retryAfterSeconds = probe.getNanosToWaitForRefill() / 1_000_000_000L;
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                    .header("Retry-After", String.valueOf(Math.max(1, retryAfterSeconds)))
                    .body(Map.of(
                            "error", "Too Many Requests",
                            "message", "Redirect rate limit exceeded. Please slow down.",
                            "retryAfterSeconds", retryAfterSeconds
                    ));
        }

        Url url = urlService.findByShortUrl(shortUrl);
        if (url == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", "Not Found", "message", "Short URL does not exist."));
        }

        // 2. Password Check & Interstitial Gate Redirection
        if (url.getPassword() != null) {
            String providedPassword = (headerPassword != null && !headerPassword.isBlank())
                    ? headerPassword
                    : queryPassword;

            if (providedPassword == null || !urlService.verifyPassword(url, providedPassword)) {
                // If accessed via a browser (accepts text/html), route to the Interstitial Gate Page
                String acceptHeader = request.getHeader("Accept");
                if (acceptHeader != null && acceptHeader.contains(MediaType.TEXT_HTML_VALUE)) {
                    return ResponseEntity.status(HttpStatus.FOUND)
                            .location(URI.create("/gate/" + shortUrl))
                            .build();
                }

                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of(
                                "error", "Unauthorized",
                                "message", "This URL is password protected. Provide password via 'X-Password' header or 'password' query parameter.",
                                "gatePage", "/gate/" + shortUrl
                        ));
            }
        }

        // 3. Resolve URL & Update Click Counters / Thresholds
        Url targetUrl;
        try {
            targetUrl = urlService.resolveAndIncrementClicks(shortUrl);
        } catch (CustomException ce) {
            return ResponseEntity.status(HttpStatus.GONE)
                    .body(Map.of("error", "Gone", "message", ce.getMessage()));
        }

        // 4. Asynchronous Event Telemetry Processing (Safe detached event)
        ClickEvent clickEvent = analyticsService.extractClickEvent(targetUrl, request);
        analyticsService.recordClickAsync(clickEvent);

        // 5. High-Speed HTTP Redirect (301 Permanent or 302 Found)
        RedirectType redirectType = targetUrl.getRedirectType() != null
                ? targetUrl.getRedirectType()
                : RedirectType.TEMPORARY_302;

        HttpHeaders headers = new HttpHeaders();
        headers.setLocation(URI.create(targetUrl.getOriginalUrl()));

        if (redirectType == RedirectType.PERMANENT_301) {
            // Browser caching allowed for 301
            headers.setCacheControl("public, max-age=86400");
        } else {
            // Guarantee future hits come to server for telemetry tracking
            headers.setCacheControl("no-cache, no-store, must-revalidate");
            headers.setPragma("no-cache");
            headers.setExpires(0);
        }

        return new ResponseEntity<>(headers, redirectType.getHttpStatus());
    }

    /**
     * Downloadable / displayable raw PNG QR code image endpoint.
     */
    @GetMapping(value = "/api/v1/url/{shortUrl}/qr", produces = MediaType.IMAGE_PNG_VALUE)
    public ResponseEntity<byte[]> getQrCode(
            @PathVariable String shortUrl,
            @RequestParam(value = "size", defaultValue = "300") int size,
            @RequestParam(value = "download", defaultValue = "false") boolean download,
            HttpServletRequest request) {

        Url url = urlService.findByShortUrl(shortUrl);
        if (url == null) {
            return ResponseEntity.notFound().build();
        }

        String targetQrContent = url.getOriginalUrl();
        byte[] qrBytes = qrCodeService.generateQrCodeBytes(targetQrContent, size, size);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.IMAGE_PNG);
        if (download) {
            headers.set(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"qrcode-" + shortUrl + ".png\"");
        } else {
            headers.set(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"qrcode-" + shortUrl + ".png\"");
        }

        return new ResponseEntity<>(qrBytes, headers, HttpStatus.OK);
    }

    /**
     * Create a single shortened URL (authenticated).
     */
    @PostMapping("/api/v1/url/save")
    public ResponseEntity<UrlResponse> createShortUrl(
            @Valid @RequestBody UrlRequest urlRequest,
            HttpServletRequest request) {

        // Rate Limiting Check
        String clientIp = request.getRemoteAddr();
        ConsumptionProbe probe = rateLimitingService.tryConsumeCreateUrl(clientIp);
        if (!probe.isConsumed()) {
            long retryAfterSeconds = probe.getNanosToWaitForRefill() / 1_000_000_000L;
            throw new CustomException("Rate limit exceeded for link creation. Try again in " + retryAfterSeconds + " seconds.");
        }

        Url savedUrl = urlService.createUrl(urlRequest);
        return ResponseEntity.status(HttpStatus.CREATED).body(urlService.toResponse(savedUrl));
    }

    /**
     * Create multiple shortened URLs in batch (authenticated).
     */
    @PostMapping("/api/v1/url/save/batch")
    public ResponseEntity<List<UrlResponse>> createBatchUrls(
            @RequestBody List<@Valid UrlRequest> urlRequests,
            HttpServletRequest request) {

        String clientIp = request.getRemoteAddr();
        ConsumptionProbe probe = rateLimitingService.tryConsumeCreateUrl(clientIp);
        if (!probe.isConsumed()) {
            throw new CustomException("Rate limit exceeded for batch link creation.");
        }

        List<Url> savedUrls = urlService.createMultipleUrls(urlRequests);
        List<UrlResponse> responses = savedUrls.stream().map(urlService::toResponse).toList();
        return ResponseEntity.status(HttpStatus.CREATED).body(responses);
    }

    /**
     * Get all URLs created by the authenticated user.
     */
    @GetMapping({"/api/v1/url/my-links", "/api/v1/url/all"})
    public ResponseEntity<List<UrlResponse>> getMyUrls() {
        return ResponseEntity.ok(urlService.getUserUrls());
    }

    /**
     * Get specific URL details by ID.
     */
    @GetMapping("/api/v1/url/get/{id}")
    public ResponseEntity<UrlResponse> getUrlById(@PathVariable Long id) {
        Url url = urlService.getOriginalUrlById(id);
        if (url == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(urlService.toResponse(url));
    }

    /**
     * Update URL settings (authenticated, owner only).
     */
    @PutMapping("/api/v1/url/update/{id}")
    public ResponseEntity<UrlResponse> updateUrl(@PathVariable Long id, @RequestBody UrlRequest urlRequest) {
        Url updatedUrl = urlService.updateUrl(id, urlRequest);
        if (updatedUrl == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(urlService.toResponse(updatedUrl));
    }

    /**
     * Toggle active/inactive status (authenticated, owner only).
     */
    @PatchMapping("/api/v1/url/{id}/toggle-status")
    public ResponseEntity<UrlResponse> toggleStatus(@PathVariable Long id) {
        Url updatedUrl = urlService.toggleStatus(id);
        if (updatedUrl == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(urlService.toResponse(updatedUrl));
    }

    /**
     * Delete a single URL (authenticated, owner only).
     */
    @DeleteMapping("/api/v1/url/delete/{id}")
    public ResponseEntity<Map<String, String>> deleteUrl(@PathVariable Long id) {
        boolean deleted = urlService.deleteUrl(id);
        if (!deleted) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(Map.of("message", "URL deleted successfully", "id", String.valueOf(id)));
    }

    /**
     * Delete all URLs belonging to the authenticated user.
     */
    @DeleteMapping("/api/v1/url/delete/all")
    public ResponseEntity<Map<String, String>> deleteAllUserUrls() {
        List<UrlResponse> userUrls = urlService.getUserUrls();
        userUrls.forEach(u -> urlService.deleteUrl(u.getId()));
        return ResponseEntity.ok(Map.of("message", "All your URLs have been deleted", "count", String.valueOf(userUrls.size())));
    }

    /**
     * Raw analytics logs for a URL (authenticated).
     */
    @GetMapping("/api/v1/url/{id}/analytics")
    public ResponseEntity<List<ClickAnalyticsResponse>> getUrlAnalytics(@PathVariable Long id) {
        return ResponseEntity.ok(analyticsService.getAnalyticsByUrlId(id));
    }

    /**
     * Demographic and time-series telemetry summary for a URL (authenticated).
     */
    @GetMapping("/api/v1/url/{id}/analytics/summary")
    public ResponseEntity<Map<String, Object>> getUrlAnalyticsSummary(@PathVariable Long id) {
        return ResponseEntity.ok(analyticsService.getAnalyticsSummary(id));
    }
}
