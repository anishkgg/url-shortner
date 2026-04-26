package in.proofofconcept.url.shortner.service;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.Refill;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class RateLimitingService {

    // Cache to store buckets per IP address
    private final Map<String, Bucket> createUrlBuckets = new ConcurrentHashMap<>();
    private final Map<String, Bucket> redirectBuckets = new ConcurrentHashMap<>();

    // Limit for creating URLs: 10 requests per hour
    public Bucket getCreateUrlBucket(String ip) {
        return createUrlBuckets.computeIfAbsent(ip, k -> {
            Bandwidth limit = Bandwidth.classic(10, Refill.greedy(10, Duration.ofHours(1)));
            return Bucket.builder().addLimit(limit).build();
        });
    }

    // Limit for redirects: 100 requests per minute
    public Bucket getRedirectBucket(String ip) {
        return redirectBuckets.computeIfAbsent(ip, k -> {
            Bandwidth limit = Bandwidth.classic(100, Refill.greedy(100, Duration.ofMinutes(1)));
            return Bucket.builder().addLimit(limit).build();
        });
    }
}
