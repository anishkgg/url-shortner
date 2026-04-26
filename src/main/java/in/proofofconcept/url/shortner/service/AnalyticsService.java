package in.proofofconcept.url.shortner.service;

import in.proofofconcept.url.shortner.dto.response.ClickAnalyticsResponse;
import in.proofofconcept.url.shortner.model.ClickAnalytics;
import in.proofofconcept.url.shortner.model.Url;
import in.proofofconcept.url.shortner.repository.ClickAnalyticsRepository;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class AnalyticsService {

    private final ClickAnalyticsRepository clickAnalyticsRepository;

    public AnalyticsService(ClickAnalyticsRepository clickAnalyticsRepository) {
        this.clickAnalyticsRepository = clickAnalyticsRepository;
    }

    @Async
    public void recordClick(Url url, HttpServletRequest request) {
        ClickAnalytics analytics = new ClickAnalytics();
        analytics.setUrl(url);
        analytics.setClickTimestamp(LocalDateTime.now());

        String userAgent = request.getHeader("User-Agent");
        analytics.setUserAgent(userAgent);
        analytics.setIpAddress(request.getRemoteAddr());
        analytics.setReferer(request.getHeader("Referer"));

        parseUserAgent(analytics, userAgent);
        analytics.setCountry(resolveCountry(request.getRemoteAddr()));

        clickAnalyticsRepository.save(analytics);
    }

    private String resolveCountry(String ip) {
        if (ip.equals("127.0.0.1") || ip.equals("0:0:0:0:0:0:0:1")) return "Localhost";
        String[] countries = {"USA", "India", "UK", "Germany", "Canada", "Japan"};
        return countries[new java.util.Random().nextInt(countries.length)];
    }

    private void parseUserAgent(ClickAnalytics analytics, String ua) {
        if (ua == null) return;

        String userAgent = ua.toLowerCase();

        // Browser
        if (userAgent.contains("edg")) analytics.setBrowser("Edge");
        else if (userAgent.contains("chrome")) analytics.setBrowser("Chrome");
        else if (userAgent.contains("safari")) analytics.setBrowser("Safari");
        else if (userAgent.contains("firefox")) analytics.setBrowser("Firefox");
        else analytics.setBrowser("Unknown");

        // OS
        if (userAgent.contains("windows")) analytics.setOperatingSystem("Windows");
        else if (userAgent.contains("mac")) analytics.setOperatingSystem("MacOS");
        else if (userAgent.contains("linux")) analytics.setOperatingSystem("Linux");
        else if (userAgent.contains("android")) analytics.setOperatingSystem("Android");
        else if (userAgent.contains("iphone")) analytics.setOperatingSystem("iOS");
        else analytics.setOperatingSystem("Unknown");

        // Device
        if (userAgent.contains("mobile") || userAgent.contains("android") || userAgent.contains("iphone")) {
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
                .build();
    }

    public Map<String, Object> getAnalyticsSummary(Long urlId) {
        List<ClickAnalytics> analytics = clickAnalyticsRepository.findByUrlId(urlId);

        Map<String, Object> summary = new HashMap<>();
        summary.put("totalClicks", (long) analytics.size());

        summary.put("browsers", analytics.stream()
                .filter(a -> a.getBrowser() != null)
                .collect(Collectors.groupingBy(ClickAnalytics::getBrowser, Collectors.counting())));

        summary.put("operatingSystems", analytics.stream()
                .filter(a -> a.getOperatingSystem() != null)
                .collect(Collectors.groupingBy(ClickAnalytics::getOperatingSystem, Collectors.counting())));

        summary.put("deviceTypes", analytics.stream()
                .filter(a -> a.getDeviceType() != null)
                .collect(Collectors.groupingBy(ClickAnalytics::getDeviceType, Collectors.counting())));

        summary.put("topCountries", analytics.stream()
                .filter(a -> a.getCountry() != null)
                .collect(Collectors.groupingBy(ClickAnalytics::getCountry, Collectors.counting())));

        // Time Series Data: Clicks per day
        Map<String, Long> timeSeries = analytics.stream()
                .collect(Collectors.groupingBy(a -> a.getClickTimestamp().toLocalDate().toString(), Collectors.counting()));
        summary.put("timeSeries", timeSeries);

        return summary;
    }
}
