package io.k48.fortyeightid.shared.config;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.BucketConfiguration;
import io.github.bucket4j.Refill;
import java.time.Duration;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RateLimitConfig {

    // In-memory bucket storage (for production, use Redis-based proxy manager)
    private final ConcurrentHashMap<String, Bucket> buckets = new ConcurrentHashMap<>();

    @Bean
    public BucketConfiguration loginRateLimit() {
        // 5 requests per 15 minutes per matricule
        Bandwidth limit = Bandwidth.classic(5, Refill.intervally(5, Duration.ofMinutes(15)));
        return BucketConfiguration.builder().addLimit(limit).build();
    }

    @Bean
    public BucketConfiguration forgotPasswordRateLimit() {
        // 3 requests per 1 hour per email
        Bandwidth limit = Bandwidth.classic(3, Refill.intervally(3, Duration.ofHours(1)));
        return BucketConfiguration.builder().addLimit(limit).build();
    }

    @Bean
    public BucketConfiguration globalIpRateLimit() {
        // 100 requests per 1 minute per IP
        Bandwidth limit = Bandwidth.classic(100, Refill.intervally(100, Duration.ofMinutes(1)));
        return BucketConfiguration.builder().addLimit(limit).build();
    }

    public Bucket getBucket(String key, BucketConfiguration config) {
        return buckets.computeIfAbsent(key, k -> Bucket.builder().addLimit(config.getBandwidths()[0]).build());
    }
}
