package io.k48.fortyeightid.shared.config;

import io.github.bucket4j.Bucket;
import io.github.bucket4j.BucketConfiguration;
import io.github.bucket4j.ConsumptionProbe;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.Duration;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Slf4j
@Component
@RequiredArgsConstructor
public class RateLimitFilter extends OncePerRequestFilter {

    private final RateLimitConfig rateLimitConfig;
    private final BucketConfiguration loginRateLimit;
    private final BucketConfiguration forgotPasswordRateLimit;
    private final BucketConfiguration globalIpRateLimit;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        String path = request.getRequestURI();
        String clientIp = getClientIp(request);

        // Apply global IP rate limit first
        if (!applyGlobalIpLimit(clientIp, response)) {
            return;
        }

        // Apply endpoint-specific rate limits
        if (path.equals("/api/v1/auth/login")) {
            String matricule = request.getParameter("matricule");
            if (matricule != null && !matricule.isBlank()) {
                if (!applyEndpointLimit("login:" + matricule, response, loginRateLimit)) {
                    return;
                }
            }
        } else if (path.equals("/api/v1/auth/forgot-password")) {
            // Use IP-based limiting for forgot-password endpoint
            if (!applyEndpointLimit("forgot-password:" + clientIp, response, forgotPasswordRateLimit)) {
                return;
            }
        }

        filterChain.doFilter(request, response);
    }

    private boolean applyGlobalIpLimit(String clientIp, HttpServletResponse response) {
        return applyEndpointLimit("ip:" + clientIp, response, globalIpRateLimit);
    }

    private boolean applyEndpointLimit(String key, HttpServletResponse response, BucketConfiguration config) {
        Bucket bucket = rateLimitConfig.getBucket(key, config);
        ConsumptionProbe probe = bucket.tryConsumeAndReturnRemaining(1);

        if (probe.isConsumed()) {
            // Add rate limit headers to response
            response.setHeader("X-RateLimit-Limit", String.valueOf(config.getBandwidths()[0].getCapacity()));
            response.setHeader("X-RateLimit-Remaining", String.valueOf(probe.getRemainingTokens()));
            response.setHeader("X-RateLimit-Reset", String.valueOf(System.currentTimeMillis() + config.getBandwidths()[0].getRefillPeriodNanos() / 1_000_000));
            return true;
        } else {
            // Rate limit exceeded
            response.setHeader("X-RateLimit-Limit", String.valueOf(config.getBandwidths()[0].getCapacity()));
            response.setHeader("X-RateLimit-Remaining", "0");
            response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            response.setContentType("application/json");
            try {
                response.getWriter().write("{\"error\":\"Too Many Requests\",\"message\":\"Rate limit exceeded.\"}");
            } catch (IOException e) {
                log.error("Failed to write rate limit response", e);
            }
            log.warn("Rate limit exceeded for key: {}", key);
            return false;
        }
    }

    private String getClientIp(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isBlank()) {
            return xForwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
