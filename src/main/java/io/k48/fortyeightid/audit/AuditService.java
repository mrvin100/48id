package io.k48.fortyeightid.audit;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.k48.fortyeightid.audit.internal.AuditLog;
import io.k48.fortyeightid.audit.internal.AuditLogRepository;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuditService {

    private final AuditLogRepository auditLogRepository;
    private final ObjectMapper objectMapper;

    @Transactional
    public void log(UUID userId, String action, Map<String, Object> details) {
        try {
            var entry = AuditLog.builder()
                    .userId(userId)
                    .action(action)
                    .details(objectMapper.writeValueAsString(details))
                    .build();
            auditLogRepository.save(entry);
        } catch (Exception ex) {
            log.error("Failed to write audit log: action={}, userId={}", action, userId, ex);
        }
    }
}
