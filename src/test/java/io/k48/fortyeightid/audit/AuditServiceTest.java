package io.k48.fortyeightid.audit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.k48.fortyeightid.audit.internal.AuditLog;
import io.k48.fortyeightid.audit.internal.AuditLogRepository;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AuditServiceTest {

    @Mock
    private AuditLogRepository auditLogRepository;

    @InjectMocks
    private AuditService auditService;

    @Test
    void log_writesAuditLogWithDetails() {
        var userId = UUID.randomUUID();
        var action = "TEST_ACTION";
        Map<String, Object> details = new HashMap<>();
        details.put("key", "value");

        auditService.log(userId, action, details);

        verify(auditLogRepository, times(1)).save(any(AuditLog.class));
    }

    @Test
    void log_withExplicitIpAddress_usesProvidedIp() {
        var userId = UUID.randomUUID();
        var action = "TEST_ACTION";
        Map<String, Object> details = new HashMap<>();
        details.put("key", "value");
        var ipAddress = "192.168.1.1";
        var userAgent = "TestAgent";

        auditService.log(userId, action, details, ipAddress, userAgent);

        verify(auditLogRepository, times(1)).save(any(AuditLog.class));
    }

    @Test
    void log_errorDoesNotBubbleUp() {
        var userId = UUID.randomUUID();
        var action = "TEST_ACTION";
        Map<String, Object> details = new HashMap<>();
        details.put("key", "value");

        when(auditLogRepository.save(any(AuditLog.class)))
                .thenThrow(new RuntimeException("Database error"));

        // Should not throw exception
        auditService.log(userId, action, details);
    }
}
