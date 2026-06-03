package in.proofofconcept.url.shortner.controller;
import in.proofofconcept.url.shortner.dto.request.UrlRequest;
import in.proofofconcept.url.shortner.dto.response.ClickAnalyticsResponse;
import in.proofofconcept.url.shortner.dto.response.UrlResponse;
import in.proofofconcept.url.shortner.exception.CustomException;
import in.proofofconcept.url.shortner.model.User;
import in.proofofconcept.url.shortner.service.AnalyticsService;
import in.proofofconcept.url.shortner.service.RateLimitingService;
import io.github.bucket4j.Bucket;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import in.proofofconcept.url.shortner.model.Url;
import in.proofofconcept.url.shortner.service.UrlService;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/v1/url/")
public class UrlController {

	private final UrlService urlService;
    private final AnalyticsService analyticsService;
    private final RateLimitingService rateLimitingService;

    @Autowired
    UrlController(UrlService urlService, AnalyticsService analyticsService, RateLimitingService rateLimitingService) {
        this.urlService = urlService;
        this.analyticsService = analyticsService;
        this.rateLimitingService = rateLimitingService;
    }

    @GetMapping("all")
   public ResponseEntity<List<UrlResponse>> getAllUrls() {
        return ResponseEntity.ok(urlService.getAllResponses());
    }

    @GetMapping("get/{id}")
    public ResponseEntity<UrlResponse> getOriginalUrlById(@PathVariable Long id) {
        Url url = urlService.getOriginalUrlById(id);
        if (url != null) {
            return ResponseEntity.ok(urlService.toResponse(url));
        }
        return ResponseEntity.notFound().build();
    }

    @GetMapping("{shortUrl}")
    public ResponseEntity<?> getOriginalUrl(@PathVariable String shortUrl, 
                                            @RequestHeader(value = "X-Password", required = false) String password,
                                            HttpServletRequest request) {
        // Rate Limiting Check
        Bucket bucket = rateLimitingService.getRedirectBucket(request.getRemoteAddr());
        if (!bucket.tryConsume(1)) {
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS).body("Too many redirect requests. Slow down!");
        }

        Url url = urlService.findByShortUrl(shortUrl);
        if(url != null) {
            // 1. Password Check
            if (url.getPassword() != null) {
                if (password == null || !urlService.verifyPassword(url, password)) {
                    return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                            .body("This URL is password protected. Please provide the correct password in the X-Password header.");
                }
            }

            // 2. Record Analytics
            analyticsService.recordClick(url, request);

            // 3. Handle One-Time Use
            UrlResponse response = urlService.toResponse(url);
            urlService.handleOneTimeUse(url);

            return ResponseEntity.status(HttpStatus.FOUND)
                    .location(URI.create(url.getOriginalUrl()))
                    .body(response);
        } else {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
    }

    @GetMapping("{id}/analytics")
    public ResponseEntity<List<ClickAnalyticsResponse>> getUrlAnalytics(@PathVariable Long id) {
        return ResponseEntity.ok(analyticsService.getAnalyticsByUrlId(id));
    }

    @GetMapping("{id}/analytics/summary")
    public ResponseEntity<java.util.Map<String, Object>> getUrlAnalyticsSummary(@PathVariable Long id) {
        return ResponseEntity.ok(analyticsService.getAnalyticsSummary(id));
    }

    

    @PostMapping("save")
    public ResponseEntity<UrlResponse> createShortUrl(@RequestBody UrlRequest urlRequest, HttpServletRequest request) {
        // Rate Limiting Check
        Bucket bucket = rateLimitingService.getCreateUrlBucket(request.getRemoteAddr());
        if (!bucket.tryConsume(1)) {
            throw new CustomException("Rate limit exceeded. You can only create 10 URLs per hour.");
        }

        Url url = urlService.fromRequest(urlRequest);
        Url savedUrl = urlService.saveUrl(url);
        return ResponseEntity.ok(urlService.toResponse(savedUrl));
    }

    @PostMapping("save/batch")
    public ResponseEntity<List<UrlResponse>> createMultipleShortUrls(@RequestBody List<UrlRequest> urlRequests) {
        List<Url> urls = urlRequests.stream().map(urlService::fromRequest).toList();
        List<Url> savedUrls = urlService.saveMultipleUrls(urls);
        List<UrlResponse> responses = savedUrls.stream().map(urlService::toResponse).toList();
        return ResponseEntity.ok(responses);
    }

    @PutMapping("update/{id}")
    public ResponseEntity<UrlResponse> updateUrl(@PathVariable Long id, @RequestBody UrlRequest urlRequest) {
        Url updatedUrl = urlService.fromRequest(urlRequest);
        Url savedUrl = urlService.updateUrl(id, updatedUrl);
        if (savedUrl != null) {
            return ResponseEntity.ok(urlService.toResponse(savedUrl));
        }
        return ResponseEntity.notFound().build();
    }

    @DeleteMapping("delete/{id}")
    public ResponseEntity<String> deleteUrl(@PathVariable Long id) {
        Url url = urlService.getOriginalUrlById(id);
        if (url == null) {
            return ResponseEntity.notFound().build();
        }
        
        // Ownership Check
        User currentUser = urlService.getCurrentUser();
        if (url.getUser() == null || !url.getUser().getId().equals(currentUser.getId())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("You do not have permission to delete this URL");
        }

        urlService.deleteUrl(id);
        return ResponseEntity.ok("Delete Successfully");
    }

    @DeleteMapping("delete/all")
    public ResponseEntity<String> deleteAll() {
        User currentUser = urlService.getCurrentUser();
        List<UrlResponse> userUrls = urlService.getAllResponses();
        userUrls.forEach(u -> urlService.deleteUrl(u.getId()));
        return ResponseEntity.ok("All your Urls have been deleted");
    }

}
