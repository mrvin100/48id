package io.k48.fortyeightid.audit.internal;

import java.time.Instant;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/audit-log")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
class AdminAuditController {

    private final AuditLogRepository auditLogRepository;

    @GetMapping
    ResponseEntity<Page<AuditLogResponse>> getAuditLog(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size,
            @RequestParam(required = false) String eventType,
            @RequestParam(required = false) UUID userId,
            @RequestParam(required = false) Instant from,
            @RequestParam(required = false) Instant to) {

        var pageable = PageRequest.of(page, size);
        var auditPage = auditLogRepository.findWithFilters(eventType, userId, from, to, pageable);
        var responsePage = auditPage.map(AuditLogResponse::from);

        return ResponseEntity.ok(responsePage);
    }
}
