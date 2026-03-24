package io.k48.fortyeightid.auth;

import io.k48.fortyeightid.audit.AuditService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.filter.OncePerRequestFilter;

public class ApiKeyAuthFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(ApiKeyAuthFilter.class);
    private static final String API_KEY_HEADER = "X-API-Key";

    private final ApiKeyManagementPort apiKeyService;
    private final AuditService auditService;
    private final String apiPrefix;

    public ApiKeyAuthFilter(ApiKeyManagementPort apiKeyService, AuditService auditService, String apiPrefix) {
        this.apiKeyService = apiKeyService;
        this.auditService = auditService;
        this.apiPrefix = apiPrefix;
    }

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

        if (org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication() != null) {
            filterChain.doFilter(request, response);
            return;
        }

        var apiKey = request.getHeader(API_KEY_HEADER);
        if (apiKey == null || apiKey.isBlank()) {
            writeForbidden(response, "Missing X-API-Key header");
            return;
        }

        var validKey = apiKeyService.validate(apiKey);
        if (validKey.isEmpty()) {
            log.debug("Invalid API key presented");
            writeForbidden(response, "Invalid X-API-Key header");
            return;
        }

        var key = validKey.get();
        var authentication = new ApiKeyAuthentication(key.getId(), key.getAppName());
        org.springframework.security.core.context.SecurityContextHolder.getContext().setAuthentication(authentication);
        apiKeyService.updateLastUsed(key);

        try {
            auditService.log(null, "API_KEY_USED", Map.of("appName", key.getAppName(), "keyId", key.getId().toString()));
        } catch (Exception ex) {
            log.warn("Failed to emit API_KEY_USED audit event for keyId={}: {}", key.getId(), ex.getMessage());
        }

        filterChain.doFilter(request, response);
    }

    private void writeForbidden(HttpServletResponse response, String error) throws IOException {
        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
        response.setContentType("application/json");
        response.getWriter().write("{\"error\":\"" + error.replace("\"", "\\\"") + "\"}");
    }
}
