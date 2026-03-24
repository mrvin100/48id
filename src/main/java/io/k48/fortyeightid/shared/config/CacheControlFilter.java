package io.k48.fortyeightid.shared.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.filter.OncePerRequestFilter;

public class CacheControlFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(CacheControlFilter.class);

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        String path = request.getRequestURI();

        // Add Cache-Control: no-store for auth endpoints
        if (path.contains("/auth/")) {
            response.setHeader("Cache-Control", "no-store, no-cache, must-revalidate, proxy-revalidate");
            response.setHeader("Pragma", "no-cache");
            response.setHeader("Expires", "0");
        }

        // Add X-Content-Type-Options: nosniff to all responses
        response.setHeader("X-Content-Type-Options", "nosniff");

        filterChain.doFilter(request, response);
    }
}
