package io.k48.fortyeightid.shared.config;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.BucketConfiguration;
import java.time.Duration;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RateLimitConfig {

    private final ConcurrentHashMap<String, Bucket> buckets = new ConcurrentHashMap<>();

    @Bean
    public BucketConfiguration loginRateLimit() {
        // 5 requests per 15 minutes per matricule
        Bandwidth limit = Bandwidth.builder()
                .capacity(5)
                .refillIntervally(5, Duration.ofMinutes(15))
                .build();
        return BucketConfiguration.builder().addLimit(limit).build();
    }

    @Bean
    public BucketConfiguration forgotPasswordRateLimit() {
        // 3 requests per 1 hour per email/IP
        Bandwidth limit = Bandwidth.builder()
                .capacity(3)
                .refillIntervally(3, Duration.ofHours(1))
                .build();
        return BucketConfiguration.builder().addLimit(limit).build();
    }

    @Bean
    public BucketConfiguration globalIpRateLimit() {
        // 100 requests per 1 minute per IP
        Bandwidth limit = Bandwidth.builder()
                .capacity(100)
                .refillIntervally(100, Duration.ofMinutes(1))
                .build();
        return BucketConfiguration.builder().addLimit(limit).build();
    }

    public Bucket getBucket(String key, BucketConfiguration config) {
        return buckets.computeIfAbsent(key, k -> Bucket.builder()
                .addLimit(config.getBandwidths()[0])
                .build());
    }
}
