package io.k48.fortyeightid.admin.internal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import io.k48.fortyeightid.audit.internal.AuditLog;
import io.k48.fortyeightid.audit.internal.AuditLogRepository;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;

@ExtendWith(MockitoExtension.class)
class AdminAuditControllerTest {

    @Mock
    private AuditLogRepository auditLogRepository;

    @InjectMocks
    private AdminAuditController adminAuditController;

    @Test
    void getAuditLog_returnsPaginatedAuditLogs() {
        // Given: Audit logs exist
        var userId = UUID.randomUUID();
        var auditLog = AuditLog.builder()
                .id(UUID.randomUUID())
                .userId(userId)
                .action("LOGIN_SUCCESS")
                .ipAddress("192.168.1.1")
                .userAgent("Mozilla/5.0")
                .createdAt(Instant.now())
                .build();

        var page = new PageImpl<>(java.util.List.of(auditLog));
        when(auditLogRepository.findWithFilters(any(), any(), any(), any(), any(Pageable.class)))
                .thenReturn(page);

        // When: Admin queries audit log
        ResponseEntity<Page<AuditLogResponse>> response = adminAuditController.getAuditLog(
                userId,
                "LOGIN_SUCCESS",
                null,
                null,
                PageRequest.of(0, 20)
        );

        // Then: Returns paginated audit logs
        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getBody().getContent()).hasSize(1);
        var log = response.getBody().getContent().get(0);
        assertThat(log.userId()).isEqualTo(userId);
        assertThat(log.action()).isEqualTo("LOGIN_SUCCESS");
        assertThat(log.ipAddress()).isEqualTo("192.168.1.1");
    }

    @Test
    void getLoginHistory_returnsLoginEvents() {
        // Given: Login audit logs exist
        var userId = UUID.randomUUID();
        var loginSuccess = AuditLog.builder()
                .id(UUID.randomUUID())
                .userId(userId)
                .action("LOGIN_SUCCESS")
                .ipAddress("192.168.1.1")
                .userAgent("Mozilla/5.0")
                .createdAt(Instant.now())
                .build();

        var page = new PageImpl<>(java.util.List.of(loginSuccess));
        when(auditLogRepository.findWithFilters(any(), any(), any(), any(), any(Pageable.class)))
                .thenReturn(page);

        // When: Admin queries login history
        ResponseEntity<Page<AuditLogResponse>> response = adminAuditController.getLoginHistory(
                userId,
                null,
                null,
                PageRequest.of(0, 20)
        );

        // Then: Returns login events
        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getBody().getContent()).hasSize(1);
        var log = response.getBody().getContent().get(0);
        assertThat(log.action()).isEqualTo("LOGIN_SUCCESS");
    }
}
