package io.k48.fortyeightid.operator.internal;

import io.k48.fortyeightid.audit.AuditLog;
import io.k48.fortyeightid.audit.AuditLogRepository;
import java.time.Instant;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("${fortyeightid.api.prefix}/operator/audit-log")
@PreAuthorize("hasRole('OPERATOR')")
@RequiredArgsConstructor
class OperatorAuditController {

    private final AuditLogRepository auditLogRepository;

    @GetMapping
    ResponseEntity<Page<OperatorAuditLogResponse>> getAuditLog(
            @RequestParam(required = false) UUID userId,
            @RequestParam(required = false) String eventType,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to,
            @PageableDefault(size = 20) Pageable pageable) {

        var page = auditLogRepository.findWithFilters(eventType, userId, from, to, pageable);
        return ResponseEntity.ok(page.map(this::toResponse));
    }

    private OperatorAuditLogResponse toResponse(AuditLog audit) {
        return new OperatorAuditLogResponse(
                audit.getId(),
                audit.getUserId(),
                audit.getAction(),
                audit.getIpAddress(),
                audit.getUserAgent(),
                audit.getCreatedAt()
        );
    }
}
