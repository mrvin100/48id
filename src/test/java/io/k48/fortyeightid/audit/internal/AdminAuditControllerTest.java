package io.k48.fortyeightid.audit.internal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

@ExtendWith(MockitoExtension.class)
class AdminAuditControllerTest {

    @Mock private AuditLogRepository auditLogRepository;
    @InjectMocks private AdminAuditController adminAuditController;

    @Test
    void getAuditLog_returnsPaginatedResults() {
        var userId = UUID.randomUUID();
        var auditLog = AuditLog.builder()
                .id(UUID.randomUUID())
                .userId(userId)
                .action("LOGIN_SUCCESS")
                .details("{}")
                .ipAddress("127.0.0.1")
                .build();

        var pageable = PageRequest.of(0, 50);
        var page = new PageImpl<>(List.of(auditLog), pageable, 1);

        when(auditLogRepository.findWithFilters(eq(null), eq(null), eq(null), eq(null), any()))
                .thenReturn(page);

        var response = adminAuditController.getAuditLog(0, 50, null, null, null, null);

        assertThat(response.getStatusCode().value()).isEqualTo(200);
        var body = response.getBody();
        assertThat(body).isNotNull();
        assertThat(body.getContent()).hasSize(1);
        assertThat(body.getContent().get(0).eventType()).isEqualTo("LOGIN_SUCCESS");
        assertThat(body.getContent().get(0).userId()).isEqualTo(userId);
    }

    @Test
    void getAuditLog_appliesFilters() {
        var userId = UUID.randomUUID();
        var eventType = "LOGIN_FAILURE";
        var from = Instant.now().minusSeconds(3600);
        var to = Instant.now();

        var pageable = PageRequest.of(0, 50);
        var page = new PageImpl<AuditLog>(List.of(), pageable, 0);

        when(auditLogRepository.findWithFilters(eq(eventType), eq(userId), eq(from), eq(to), any()))
                .thenReturn(page);

        var response = adminAuditController.getAuditLog(0, 50, eventType, userId, from, to);

        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getBody()).isNotNull();
    }
}
