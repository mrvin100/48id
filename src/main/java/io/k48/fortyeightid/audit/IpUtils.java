package io.k48.fortyeightid.audit;

import jakarta.servlet.http.HttpServletRequest;

/**
 * Utility class for IP address extraction from HTTP requests.
 */
public final class IpUtils {

    private IpUtils() {
        // Utility class - prevent instantiation
    }

    /**
     * Extracts the client IP address from an HTTP request.
     * Handles X-Forwarded-For header for reverse proxy scenarios.
     * 
     * @param request the HTTP request
     * @return the client IP address
     */
    public static String extractIpAddress(HttpServletRequest request) {
        var xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isBlank()) {
            // Take the leftmost IP (original client)
            return xForwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
