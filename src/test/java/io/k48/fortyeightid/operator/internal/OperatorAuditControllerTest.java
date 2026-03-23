package io.k48.fortyeightid.operator.internal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.k48.fortyeightid.audit.AuditLog;
import io.k48.fortyeightid.audit.AuditLogRepository;
import java.time.Instant;
import java.util.List;
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
class OperatorAuditControllerTest {

    @Mock
    private AuditLogRepository auditLogRepository;

    @InjectMocks
    private OperatorAuditController operatorAuditController;

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

        var page = new PageImpl<>(List.of(auditLog));
        when(auditLogRepository.findWithFilters(any(), any(), any(), any(), any(Pageable.class)))
                .thenReturn(page);

        // When: Operator queries audit log
        ResponseEntity<Page<OperatorAuditLogResponse>> response = operatorAuditController.getAuditLog(
                userId, "LOGIN_SUCCESS", null, null, PageRequest.of(0, 20));

        // Then: Returns paginated audit logs with 200 OK
        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getContent()).hasSize(1);
        assertThat(response.getBody().getContent().get(0).action()).isEqualTo("LOGIN_SUCCESS");
        verify(auditLogRepository, times(1)).findWithFilters(
                eq("LOGIN_SUCCESS"), eq(userId), any(), any(), any(Pageable.class));
    }

    @Test
    void getAuditLog_withNoFilters_returnsAllLogs() {
        // Given: Audit logs exist with no filter applied
        var page = new PageImpl<>(List.of(
                AuditLog.builder().id(UUID.randomUUID()).action("LOGIN_SUCCESS").createdAt(Instant.now()).build(),
                AuditLog.builder().id(UUID.randomUUID()).action("PASSWORD_RESET").createdAt(Instant.now()).build()
        ));
        when(auditLogRepository.findWithFilters(any(), any(), any(), any(), any(Pageable.class)))
                .thenReturn(page);

        // When: Operator queries without filters
        ResponseEntity<Page<OperatorAuditLogResponse>> response = operatorAuditController.getAuditLog(
                null, null, null, null, PageRequest.of(0, 20));

        // Then: Returns all logs
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getContent()).hasSize(2);
    }
}
