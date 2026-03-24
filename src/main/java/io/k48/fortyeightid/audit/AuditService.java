package io.k48.fortyeightid.audit;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuditService {

    private final AuditLogRepository auditLogRepository;
    private final ObjectMapper objectMapper;

    @Transactional
    public void log(UUID userId, String action, Map<String, Object> details) {
        log(userId, action, details, null, null);
    }

    @Transactional
    public void log(UUID userId, String action, Map<String, Object> details,
                    String ipAddress, String userAgent) {
        try {
            // Auto-capture from request context if not provided
            String capturedIp = ipAddress;
            String capturedUserAgent = userAgent;

            if (capturedIp == null || capturedUserAgent == null) {
                var requestAttributes = RequestContextHolder.getRequestAttributes();
                if (requestAttributes instanceof ServletRequestAttributes) {
                    var request = ((ServletRequestAttributes) requestAttributes).getRequest();
                    if (capturedIp == null) {
                        capturedIp = extractIpAddress(request);
                    }
                    if (capturedUserAgent == null) {
                        capturedUserAgent = request.getHeader("User-Agent");
                    }
                }
            }

            var entry = AuditLog.builder()
                    .userId(userId)
                    .action(action)
                    .details(objectMapper.writeValueAsString(details))
                    .ipAddress(capturedIp)
                    .userAgent(capturedUserAgent)
                    .build();
            auditLogRepository.save(entry);
        } catch (Exception ex) {
            log.error("Failed to write audit log: action={}, userId={}", action, userId, ex);
        }
    }

    private String extractIpAddress(jakarta.servlet.http.HttpServletRequest request) {
        var xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isBlank()) {
            // Take the leftmost IP (original client)
            return xForwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
