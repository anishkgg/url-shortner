package in.proofofconcept.url.shortner.service;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.ConsumptionProbe;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class RateLimitingService {

    private final Map<String, Bucket> createUrlBuckets = new ConcurrentHashMap<>();
    private final Map<String, Bucket> redirectBuckets = new ConcurrentHashMap<>();
    private final Map<String, Bucket> authBuckets = new ConcurrentHashMap<>();

    /**
     * Rate limit for creating URLs: 30 requests per hour per IP.
     */
    public Bucket getCreateUrlBucket(String ip) {
        return createUrlBuckets.computeIfAbsent(ip, k -> {
            Bandwidth limit = Bandwidth.builder()
                    .capacity(30)
                    .refillGreedy(30, Duration.ofHours(1))
                    .build();
            return Bucket.builder().addLimit(limit).build();
        });
    }

    /**
     * Rate limit for redirects: 120 requests per minute per IP.
     */
    public Bucket getRedirectBucket(String ip) {
        return redirectBuckets.computeIfAbsent(ip, k -> {
            Bandwidth limit = Bandwidth.builder()
                    .capacity(120)
                    .refillGreedy(120, Duration.ofMinutes(1))
                    .build();
            return Bucket.builder().addLimit(limit).build();
        });
    }

    /**
     * Rate limit for auth (login/register): 10 requests per minute per IP to mitigate brute force.
     */
    public Bucket getAuthBucket(String ip) {
        return authBuckets.computeIfAbsent(ip, k -> {
            Bandwidth limit = Bandwidth.builder()
                    .capacity(10)
                    .refillGreedy(10, Duration.ofMinutes(1))
                    .build();
            return Bucket.builder().addLimit(limit).build();
        });
    }

    public ConsumptionProbe tryConsumeRedirect(String ip) {
        return getRedirectBucket(ip).tryConsumeAndReturnRemaining(1);
    }

    public ConsumptionProbe tryConsumeCreateUrl(String ip) {
        return getCreateUrlBucket(ip).tryConsumeAndReturnRemaining(1);
    }

    public ConsumptionProbe tryConsumeAuth(String ip) {
        return getAuthBucket(ip).tryConsumeAndReturnRemaining(1);
    }
}
