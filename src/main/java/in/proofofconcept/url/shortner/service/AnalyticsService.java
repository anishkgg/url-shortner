package in.proofofconcept.url.shortner.service;

import in.proofofconcept.url.shortner.dto.event.ClickEvent;
import in.proofofconcept.url.shortner.dto.response.ClickAnalyticsResponse;
import in.proofofconcept.url.shortner.model.ClickAnalytics;
import in.proofofconcept.url.shortner.model.Url;
import in.proofofconcept.url.shortner.repository.ClickAnalyticsRepository;
import in.proofofconcept.url.shortner.repository.UrlRepository;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Slf4j
public class AnalyticsService {

    private final ClickAnalyticsRepository clickAnalyticsRepository;
    private final UrlRepository urlRepository;

    public AnalyticsService(ClickAnalyticsRepository clickAnalyticsRepository, UrlRepository urlRepository) {
        this.clickAnalyticsRepository = clickAnalyticsRepository;
        this.urlRepository = urlRepository;
    }

    /**
     * Extracts an immutable ClickEvent from the incoming HttpServletRequest synchronously
     * within the web thread before request recycling.
     */
    public ClickEvent extractClickEvent(Url url, HttpServletRequest request) {
        String ip = extractClientIp(request);
        String userAgent = request.getHeader("User-Agent");
        String referer = request.getHeader("Referer");
        String countryHint = request.getHeader("CF-IPCountry");
        if (countryHint == null || countryHint.isBlank()) {
            countryHint = request.getHeader("X-Country-Code");
        }

        return ClickEvent.builder()
                .urlId(url.getId())
                .shortUrl(url.getShortUrl())
                .ipAddress(ip)
                .userAgent(userAgent != null ? userAgent : "Unknown")
                .referer(referer != null && !referer.isBlank() ? referer : "Direct")
                .countryHint(countryHint)
                .timestamp(LocalDateTime.now())
                .build();
    }

    /**
     * Asynchronously records and processes the click telemetry event.
     */
    @Async
    @Transactional
    public void recordClickAsync(ClickEvent event) {
        try {
            Url url = urlRepository.findById(event.getUrlId()).orElse(null);
            if (url == null) {
                log.warn("Cannot record click analytics: URL with id {} not found", event.getUrlId());
                return;
            }

            ClickAnalytics analytics = ClickAnalytics.builder()
                    .url(url)
                    .clickTimestamp(event.getTimestamp())
                    .ipAddress(event.getIpAddress())
                    .userAgent(event.getUserAgent())
                    .referer(event.getReferer())
                    .country(resolveCountry(event.getIpAddress(), event.getCountryHint()))
                    .build();

            parseUserAgent(analytics, event.getUserAgent());
            clickAnalyticsRepository.save(analytics);
        } catch (Exception e) {
            log.error("Error processing click telemetry event for urlId {}: {}", event.getUrlId(), e.getMessage(), e);
        }
    }

    private String extractClientIp(HttpServletRequest request) {
        String[] headers = {
                "X-Forwarded-For",
                "Proxy-Client-IP",
                "WL-Proxy-Client-IP",
                "HTTP_X_FORWARDED_FOR",
                "HTTP_X_FORWARDED",
                "HTTP_X_CLUSTER_CLIENT_IP",
                "HTTP_CLIENT_IP",
                "HTTP_FORWARDED_FOR",
                "HTTP_FORWARDED",
                "HTTP_VIA",
                "REMOTE_ADDR"
        };

        for (String header : headers) {
            String ip = request.getHeader(header);
            if (ip != null && !ip.isBlank() && !"unknown".equalsIgnoreCase(ip)) {
                // Return first IP if comma-separated
                if (ip.contains(",")) {
                    return ip.split(",")[0].trim();
                }
                return ip.trim();
            }
        }
        return request.getRemoteAddr();
    }

    private String resolveCountry(String ip, String countryHint) {
        if (countryHint != null && !countryHint.isBlank() && !"XX".equalsIgnoreCase(countryHint)) {
            return countryHint.toUpperCase();
        }

        if (ip == null || ip.equals("127.0.0.1") || ip.equals("0:0:0:0:0:0:0:1") || ip.startsWith("192.168.") || ip.startsWith("10.")) {
            return "Localhost / Private Network";
        }

        return "Unknown Location";
    }

    private void parseUserAgent(ClickAnalytics analytics, String ua) {
        if (ua == null || ua.isBlank() || ua.equalsIgnoreCase("Unknown")) {
            analytics.setBrowser("Unknown");
            analytics.setOperatingSystem("Unknown");
            analytics.setDeviceType("Unknown");
            return;
        }

        String userAgent = ua.toLowerCase();

        // 1. Detect Bots & Crawlers
        if (userAgent.contains("bot") || userAgent.contains("crawler") || userAgent.contains("spider") ||
                userAgent.contains("curl") || userAgent.contains("wget") || userAgent.contains("postman")) {
            analytics.setDeviceType("Bot / Crawler");
            analytics.setBrowser("Bot");
            analytics.setOperatingSystem("Other");
            return;
        }

        // 2. Browser Detection (Order matters)
        if (userAgent.contains("edg/") || userAgent.contains("edge/")) {
            analytics.setBrowser("Microsoft Edge");
        } else if (userAgent.contains("opr/") || userAgent.contains("opera")) {
            analytics.setBrowser("Opera");
        } else if (userAgent.contains("samsungbrowser")) {
            analytics.setBrowser("Samsung Internet");
        } else if (userAgent.contains("chrome") && !userAgent.contains("chromium")) {
            analytics.setBrowser("Google Chrome");
        } else if (userAgent.contains("firefox")) {
            analytics.setBrowser("Mozilla Firefox");
        } else if (userAgent.contains("safari") && !userAgent.contains("chrome")) {
            analytics.setBrowser("Apple Safari");
        } else {
            analytics.setBrowser("Other Browser");
        }

        // 3. Operating System Detection
        if (userAgent.contains("windows")) {
            analytics.setOperatingSystem("Windows");
        } else if (userAgent.contains("macintosh") || (userAgent.contains("mac os x") && !userAgent.contains("iphone") && !userAgent.contains("ipad"))) {
            analytics.setOperatingSystem("macOS");
        } else if (userAgent.contains("android")) {
            analytics.setOperatingSystem("Android");
        } else if (userAgent.contains("iphone")) {
            analytics.setOperatingSystem("iOS (iPhone)");
        } else if (userAgent.contains("ipad")) {
            analytics.setOperatingSystem("iPadOS");
        } else if (userAgent.contains("cros")) {
            analytics.setOperatingSystem("ChromeOS");
        } else if (userAgent.contains("linux")) {
            analytics.setOperatingSystem("Linux");
        } else {
            analytics.setOperatingSystem("Other OS");
        }

        // 4. Device Type
        if (userAgent.contains("ipad") || userAgent.contains("tablet")) {
            analytics.setDeviceType("Tablet");
        } else if (userAgent.contains("mobile") || userAgent.contains("iphone") || userAgent.contains("android")) {
            analytics.setDeviceType("Mobile");
        } else {
            analytics.setDeviceType("Desktop");
        }
    }

    public List<ClickAnalyticsResponse> getAnalyticsByUrlId(Long urlId) {
        return clickAnalyticsRepository.findByUrlId(urlId)
                .stream()
                .map(this::toAnalyticsResponse)
                .toList();
    }

    private ClickAnalyticsResponse toAnalyticsResponse(ClickAnalytics a) {
        return ClickAnalyticsResponse.builder()
                .id(a.getId())
                .clickTimestamp(a.getClickTimestamp())
                .userAgent(a.getUserAgent())
                .ipAddress(a.getIpAddress())
                .referer(a.getReferer())
                .browser(a.getBrowser())
                .operatingSystem(a.getOperatingSystem())
                .deviceType(a.getDeviceType())
                .country(a.getCountry())
                .build();
    }

    public Map<String, Object> getAnalyticsSummary(Long urlId) {
        List<ClickAnalytics> analytics = clickAnalyticsRepository.findByUrlId(urlId);

        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("urlId", urlId);
        summary.put("totalClicks", (long) analytics.size());

        // Breakdown by Browser
        summary.put("browsers", analytics.stream()
                .filter(a -> a.getBrowser() != null)
                .collect(Collectors.groupingBy(ClickAnalytics::getBrowser, Collectors.counting())));

        // Breakdown by OS
        summary.put("operatingSystems", analytics.stream()
                .filter(a -> a.getOperatingSystem() != null)
                .collect(Collectors.groupingBy(ClickAnalytics::getOperatingSystem, Collectors.counting())));

        // Breakdown by Device Type
        summary.put("deviceTypes", analytics.stream()
                .filter(a -> a.getDeviceType() != null)
                .collect(Collectors.groupingBy(ClickAnalytics::getDeviceType, Collectors.counting())));

        // Breakdown by Country
        summary.put("countries", analytics.stream()
                .filter(a -> a.getCountry() != null)
                .collect(Collectors.groupingBy(ClickAnalytics::getCountry, Collectors.counting())));

        // Breakdown by Referrer
        summary.put("referrers", analytics.stream()
                .filter(a -> a.getReferer() != null)
                .collect(Collectors.groupingBy(ClickAnalytics::getReferer, Collectors.counting())));

        // Daily Time Series Data
        Map<String, Long> dailyTimeSeries = analytics.stream()
                .filter(a -> a.getClickTimestamp() != null)
                .collect(Collectors.groupingBy(a -> a.getClickTimestamp().toLocalDate().toString(), TreeMap::new, Collectors.counting()));
        summary.put("dailyTimeSeries", dailyTimeSeries);

        return summary;
    }
}
