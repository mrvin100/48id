package io.k48.fortyeightid.auth;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Slf4j
@Component
@RequiredArgsConstructor
public class ApiKeyAuthFilter extends OncePerRequestFilter {

    private static final String API_KEY_HEADER = "X-API-Key";

    private final ApiKeyManagementPort apiKeyService;

    @Value("${fortyeightid.api.prefix:/api/v1}")
    private String apiPrefix = "/api/v1";

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        var uri = request.getRequestURI();
        if ((apiPrefix + "/auth/verify-token").equals(uri)) {
            return false;
        }
        return !(uri.startsWith(apiPrefix + "/users/") && (uri.endsWith("/identity") || uri.endsWith("/exists")));
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        if (SecurityContextHolder.getContext().getAuthentication() != null) {
            filterChain.doFilter(request, response);
            return;
        }

        var apiKey = request.getHeader(API_KEY_HEADER);
        if (apiKey == null || apiKey.isBlank()) {
            writeForbidden(response, "Missing X-API-Key header", "Requires X-API-Key header. Contact K48 admin to obtain an API key.");
            return;
        }

        var validKey = apiKeyService.validate(apiKey);
        if (validKey.isEmpty()) {
            log.debug("Invalid API key presented");
            writeForbidden(response, "Invalid X-API-Key header", "The supplied API key is invalid or inactive.");
            return;
        }

        var key = validKey.get();
        var authentication = new ApiKeyAuthentication(key.getId(), key.getAppName());
        SecurityContextHolder.getContext().setAuthentication(authentication);
        apiKeyService.updateLastUsed(key);

        filterChain.doFilter(request, response);
    }

    private void writeForbidden(HttpServletResponse response, String error, String message) throws IOException {
        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
        response.setContentType("application/json");
        response.getWriter().write("{\"error\":\"" + error + "\",\"message\":\"" + message + "\"}");
    }
}
