package io.k48.fortyeightid.auth;

import io.k48.fortyeightid.audit.AuditService;
import io.k48.fortyeightid.auth.internal.UserPrincipal;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

public class OperatorActionAuditFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(OperatorActionAuditFilter.class);
    private static final SimpleGrantedAuthority ROLE_OPERATOR = new SimpleGrantedAuthority("ROLE_OPERATOR");

    private final AuditService auditService;
    private final String apiPrefix;

    public OperatorActionAuditFilter(AuditService auditService, String apiPrefix) {
        this.auditService = auditService;
        this.apiPrefix = apiPrefix;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        if (request.getRequestURI().startsWith(apiPrefix + "/operator/")) {
            var authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication != null && authentication.getAuthorities().contains(ROLE_OPERATOR)) {
                emitAuditEvent(authentication, request);
            }
        }

        filterChain.doFilter(request, response);
    }

    private void emitAuditEvent(org.springframework.security.core.Authentication authentication,
                                HttpServletRequest request) {
        try {
            if (!(authentication.getPrincipal() instanceof UserPrincipal principal)) {
                log.warn("OPERATOR_ACTION skipped: unexpected principal type={}", authentication.getPrincipal().getClass().getSimpleName());
                return;
            }
            auditService.log(
                    principal.getId(),
                    "OPERATOR_ACTION",
                    Map.of(
                            "endpoint", request.getRequestURI(),
                            "method", request.getMethod(),
                            "matricule", principal.getMatricule()
                    )
            );
        } catch (Exception ex) {
            log.warn("Failed to emit OPERATOR_ACTION audit event: endpoint={}, method={}, cause={}",
                    request.getRequestURI(), request.getMethod(), ex.getClass().getSimpleName());
        }
    }
}
