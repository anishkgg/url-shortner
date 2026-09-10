package in.proofofconcept.url.shortner.controller;

import in.proofofconcept.url.shortner.dto.request.VerifyPasswordRequest;
import in.proofofconcept.url.shortner.exception.CustomException;
import in.proofofconcept.url.shortner.model.Url;
import in.proofofconcept.url.shortner.service.AnalyticsService;
import in.proofofconcept.url.shortner.service.UrlService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.Map;

@RestController
public class PasswordGateController {

    private final UrlService urlService;
    private final AnalyticsService analyticsService;

    public PasswordGateController(UrlService urlService, AnalyticsService analyticsService) {
        this.urlService = urlService;
        this.analyticsService = analyticsService;
    }

    /**
     * Serves an interstitial gate page in the browser requesting password entry.
     */
    @GetMapping(value = "/gate/{shortUrl}", produces = MediaType.TEXT_HTML_VALUE)
    public ResponseEntity<String> showGatePage(@PathVariable String shortUrl,
                                              @RequestParam(value = "error", required = false) String error) {
        Url url = urlService.findByShortUrl(shortUrl);
        if (url == null || url.getPassword() == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body("<h1>Link Not Found or Not Protected</h1>");
        }

        String errorMessageHtml = (error != null)
                ? "<div class='error-banner'>Incorrect password. Please try again.</div>"
                : "";

        String html = """
                <!DOCTYPE html>
                <html lang="en">
                <head>
                    <meta charset="UTF-8">
                    <meta name="viewport" content="width=device-width, initial-scale=1.0">
                    <title>Password Protected Link - Verification Gate</title>
                    <style>
                        * { box-sizing: border-box; margin: 0; padding: 0; font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, Helvetica, Arial, sans-serif; }
                        body { background: #0f172a; color: #f8fafc; min-height: 100vh; display: flex; align-items: center; justify-content: center; padding: 20px; }
                        .card { background: #1e293b; border: 1px solid #334155; border-radius: 16px; padding: 40px; max-width: 440px; width: 100%; box-shadow: 0 20px 25px -5px rgba(0,0,0,0.5); text-align: center; }
                        .lock-icon { width: 56px; height: 56px; background: #3b82f6; border-radius: 50%; display: inline-flex; align-items: center; justify-content: center; margin-bottom: 20px; font-size: 26px; }
                        h2 { font-size: 22px; font-weight: 700; margin-bottom: 8px; color: #ffffff; }
                        p { color: #94a3b8; font-size: 14px; margin-bottom: 24px; line-height: 1.5; }
                        .short-badge { background: #0f172a; padding: 6px 14px; border-radius: 20px; display: inline-block; font-family: monospace; font-size: 13px; color: #38bdf8; margin-bottom: 20px; }
                        .error-banner { background: #7f1d1d; border: 1px solid #dc2626; color: #fca5a5; padding: 10px; border-radius: 8px; font-size: 13px; margin-bottom: 20px; }
                        .input-group { margin-bottom: 20px; text-align: left; }
                        label { display: block; font-size: 13px; font-weight: 600; color: #cbd5e1; margin-bottom: 6px; }
                        input[type="password"] { width: 100%; padding: 12px 16px; background: #0f172a; border: 1px solid #475569; border-radius: 8px; color: #ffffff; font-size: 15px; outline: none; transition: border-color 0.2s; }
                        input[type="password"]:focus { border-color: #3b82f6; }
                        button { width: 100%; padding: 12px 16px; background: #2563eb; color: #ffffff; border: none; border-radius: 8px; font-size: 15px; font-weight: 600; cursor: pointer; transition: background 0.2s; }
                        button:hover { background: #1d4ed8; }
                        .footer { margin-top: 24px; font-size: 12px; color: #64748b; }
                    </style>
                </head>
                <body>
                    <div class="card">
                        <div class="lock-icon">&#128274;</div>
                        <h2>Protected Destination</h2>
                        <p>The creator has protected this short link with a password. Please enter the password to proceed.</p>
                        <div class="short-badge">/${shortUrl}</div>
                        ${errorMessageHtml}
                        <form method="POST" action="/gate/${shortUrl}/submit">
                            <div class="input-group">
                                <label for="password">Password</label>
                                <input type="password" id="password" name="password" placeholder="Enter password..." required autofocus />
                            </div>
                            <button type="submit">Unlock & Continue &rarr;</button>
                        </form>
                        <div class="footer">Secured by URL Shortener Engine</div>
                    </div>
                </body>
                </html>
                """
                .replace("${shortUrl}", shortUrl)
                .replace("${errorMessageHtml}", errorMessageHtml);

        return ResponseEntity.ok(html);
    }

    /**
     * Browser form submission endpoint for the gate page.
     */
    @PostMapping(value = "/gate/{shortUrl}/submit", consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
    public ResponseEntity<?> submitGatePassword(@PathVariable String shortUrl,
                                                @RequestParam("password") String password,
                                                HttpServletRequest request) {
        Url url = urlService.findByShortUrl(shortUrl);
        if (url == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }

        if (!urlService.verifyPassword(url, password)) {
            // Redirect back to gate page with error parameter
            return ResponseEntity.status(HttpStatus.FOUND)
                    .location(URI.create("/gate/" + shortUrl + "?error=invalid"))
                    .build();
        }

        // Successfully verified: resolve and increment clicks, record analytics
        Url target = urlService.resolveAndIncrementClicks(shortUrl);
        analyticsService.recordClickAsync(analyticsService.extractClickEvent(target, request));

        return ResponseEntity.status(target.getRedirectType().getHttpStatus())
                .location(URI.create(target.getOriginalUrl()))
                .build();
    }

    /**
     * JSON API verification endpoint for programmatic access.
     */
    @PostMapping("/api/v1/url/{shortUrl}/verify")
    public ResponseEntity<?> verifyPasswordApi(@PathVariable String shortUrl,
                                               @Valid @RequestBody VerifyPasswordRequest request,
                                               HttpServletRequest servletRequest) {
        Url url = urlService.findByShortUrl(shortUrl);
        if (url == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }

        if (!urlService.verifyPassword(url, request.getPassword())) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Unauthorized", "message", "Invalid password for this protected URL"));
        }

        Url target = urlService.resolveAndIncrementClicks(shortUrl);
        analyticsService.recordClickAsync(analyticsService.extractClickEvent(target, servletRequest));

        return ResponseEntity.ok(Map.of(
                "verified", true,
                "shortUrl", target.getShortUrl(),
                "originalUrl", target.getOriginalUrl(),
                "redirectType", target.getRedirectType()
        ));
    }
}
