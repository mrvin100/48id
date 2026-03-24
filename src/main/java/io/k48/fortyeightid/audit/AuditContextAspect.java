package io.k48.fortyeightid.audit;

import io.k48.fortyeightid.shared.JwtAuthenticationDetails;
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
 * AOP aspect that automatically captures audit events from annotated methods,
 * and emits OPERATOR_ACTION for every ROLE_OPERATOR controller invocation.
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
        captureRequestContext();
        Object result = joinPoint.proceed();
        UUID userId = extractUserId(joinPoint, auditEvent.userIdExpression());
        if (userId != null) {
            auditService.log(userId, auditEvent.type(), Map.of(),
                    auditContext.getIpAddress(), auditContext.getUserAgent());
        } else {
            log.warn("Audit event '{}' skipped: could not extract user ID.", auditEvent.type());
        }
        return result;
    }

    /**
     * Emits OPERATOR_ACTION for every @RestController method called by a ROLE_OPERATOR principal.
     */
    @Around("within(@org.springframework.web.bind.annotation.RestController *)")
    public Object captureOperatorAction(ProceedingJoinPoint joinPoint) throws Throwable {
        Object result = joinPoint.proceed();

        var authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null) return result;

        boolean isOperator = authentication.getAuthorities().stream()
                .anyMatch(a -> "ROLE_OPERATOR".equals(a.getAuthority()));
        if (!isOperator) return result;

        var requestAttributes = RequestContextHolder.getRequestAttributes();
        if (!(requestAttributes instanceof ServletRequestAttributes servletAttrs)) return result;

        HttpServletRequest request = servletAttrs.getRequest();
        String endpoint = request.getRequestURI();
        String method = request.getMethod();

        UUID userId = null;
        try {
            userId = UUID.fromString(authentication.getName());
        } catch (IllegalArgumentException ignored) {
            // principal is not a UUID — skip
            return result;
        }

        String matricule = "unknown";
        if (authentication.getDetails() instanceof JwtAuthenticationDetails details) {
            matricule = details.getMatricule() != null ? details.getMatricule() : "unknown";
        }

        auditService.log(userId, "OPERATOR_ACTION", Map.of(
                "endpoint", endpoint,
                "method", method,
                "userId", userId.toString(),
                "matricule", matricule));

        return result;
    }

    private void captureRequestContext() {
        var requestAttributes = RequestContextHolder.getRequestAttributes();
        if (requestAttributes instanceof ServletRequestAttributes servletAttrs) {
            var request = servletAttrs.getRequest();
            auditContext.setIpAddress(IpUtils.extractIpAddress(request));
            auditContext.setUserAgent(request.getHeader("User-Agent"));
        }
    }

    private UUID extractUserId(ProceedingJoinPoint joinPoint, String userIdExpression) {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null) {
            try {
                return UUID.fromString(authentication.getName());
            } catch (IllegalArgumentException ignored) {
                // not a UUID
            }
        }
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
