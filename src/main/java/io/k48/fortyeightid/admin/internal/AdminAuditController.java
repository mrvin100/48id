package io.k48.fortyeightid.admin.internal;

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
@RequestMapping("${fortyeightid.api.prefix}/admin/audit-log")
@PreAuthorize("hasRole('ADMIN') or hasRole('SYSTEM_OPERATOR')")
@RequiredArgsConstructor
class AdminAuditController {

    private final AuditLogRepository auditLogRepository;

    @GetMapping
    ResponseEntity<Page<AuditLogResponse>> getAuditLog(
            @RequestParam(required = false) UUID userId,
            @RequestParam(required = false) String eventType,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to,
            @PageableDefault(size = 20) Pageable pageable) {

        var auditPage = auditLogRepository.findWithFilters(eventType, userId, from, to, pageable);

        var responsePage = auditPage.map(this::toResponse);

        return ResponseEntity.ok(responsePage);
    }

    @GetMapping("/login-history")
    ResponseEntity<Page<AuditLogResponse>> getLoginHistory(
            @RequestParam UUID userId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to,
            @PageableDefault(size = 20) Pageable pageable) {

        var auditPage = auditLogRepository.findLoginHistory(userId, from, to, pageable);

        var responsePage = auditPage.map(this::toResponse);

        return ResponseEntity.ok(responsePage);
    }

    private AuditLogResponse toResponse(AuditLog audit) {
        return new AuditLogResponse(
                audit.getId(),
                audit.getUserId(),
                audit.getAction(),
                audit.getIpAddress(),
                audit.getUserAgent(),
                audit.getCreatedAt()
        );
    }
}
