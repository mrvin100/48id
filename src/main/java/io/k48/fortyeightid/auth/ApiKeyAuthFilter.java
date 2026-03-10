package io.k48.fortyeightid.auth;

import io.k48.fortyeightid.auth.internal.ApiKeyService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Slf4j
@Component
@RequiredArgsConstructor
public class ApiKeyAuthFilter extends OncePerRequestFilter {

    private static final String API_KEY_HEADER = "X-API-Key";

    private final ApiKeyService apiKeyService;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        // Only process if no authentication already set (JWT takes priority)
        if (SecurityContextHolder.getContext().getAuthentication() != null) {
            filterChain.doFilter(request, response);
            return;
        }

        var apiKey = request.getHeader(API_KEY_HEADER);
        if (apiKey == null || apiKey.isBlank()) {
            filterChain.doFilter(request, response);
            return;
        }

        var validKey = apiKeyService.validate(apiKey);
        if (validKey.isPresent()) {
            var key = validKey.get();
            var authentication = new ApiKeyAuthentication(key.getId(), key.getAppName());
            SecurityContextHolder.getContext().setAuthentication(authentication);
            
            // Update lastUsedAt timestamp
            apiKeyService.updateLastUsed(key);
        } else {
            log.debug("Invalid API key presented");
        }

        filterChain.doFilter(request, response);
    }
}
