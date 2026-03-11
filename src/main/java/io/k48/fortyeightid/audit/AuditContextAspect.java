package io.k48.fortyeightid.audit;

import io.k48.fortyeightid.audit.internal.AuditLogRepository;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * AOP aspect that automatically captures audit events from annotated methods.
 * Extracts IP address and user agent from HTTP request context and logs
 * audit entries without boilerplate in service methods.
 */
@Aspect
@Component
@Slf4j
@RequiredArgsConstructor
public class AuditContextAspect {

    private final AuditService auditService;
    private final AuditContext auditContext;

    @Around("@annotation(auditEvent)")
    public Object captureAuditEvent(ProceedingJoinPoint joinPoint, AuditEvent auditEvent) throws Throwable {
        // Capture IP and user agent from request context
        captureRequestContext();

        // Execute the method
        Object result = joinPoint.proceed();

        // Extract user ID from method parameters or result
        UUID userId = extractUserId(joinPoint, auditEvent.userIdExpression());

        // Log the audit event
        if (userId != null) {
            auditService.log(userId, auditEvent.type(), Map.<String, Object>of(), 
                auditContext.getIpAddress(), auditContext.getUserAgent());
        }

        return result;
    }

    private void captureRequestContext() {
        var requestAttributes = RequestContextHolder.getRequestAttributes();
        if (requestAttributes instanceof ServletRequestAttributes) {
            var request = ((ServletRequestAttributes) requestAttributes).getRequest();
            auditContext.setIpAddress(extractIpAddress(request));
            auditContext.setUserAgent(request.getHeader("User-Agent"));
        }
    }

    private String extractIpAddress(HttpServletRequest request) {
        var xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isBlank()) {
            // Take the leftmost IP (original client)
            return xForwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    private UUID extractUserId(ProceedingJoinPoint joinPoint, String userIdExpression) {
        // Try to get user ID from security context first
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof org.springframework.security.core.userdetails.UserDetails) {
            try {
                return UUID.fromString(authentication.getName());
            } catch (IllegalArgumentException e) {
                // Name is not a UUID, try other methods
            }
        }

        // Try to extract from method parameters
        var signature = (MethodSignature) joinPoint.getSignature();
        var parameterNames = signature.getParameterNames();
        var args = joinPoint.getArgs();

        for (int i = 0; i < parameterNames.length; i++) {
            if (parameterNames[i].equals("userId") && args[i] instanceof UUID) {
                return (UUID) args[i];
            }
        }

        return null;
    }
}
