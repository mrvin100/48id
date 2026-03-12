package io.k48.fortyeightid.shared.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Slf4j
@Component
public class CacheControlFilter extends OncePerRequestFilter {

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
