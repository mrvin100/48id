package io.k48.fortyeightid.operator.internal;

import io.k48.fortyeightid.audit.AuditLog;
import io.k48.fortyeightid.audit.AuditLogRepository;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
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
    private final OperatorAccountService operatorAccountService;

    /**
     * Returns audit log scoped to the members of the given operator account.
     * Caller must be an active member of that account.
     */
    @GetMapping
    ResponseEntity<Page<OperatorAuditLogResponse>> getAuditLog(
            @RequestParam UUID accountId,
            @RequestParam(required = false) UUID userId,
            @RequestParam(required = false) String eventType,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to,
            @PageableDefault(size = 20) Pageable pageable,
            @AuthenticationPrincipal String callerId) {

        // Verify caller is a member of this account
        operatorAccountService.requireActiveMember(accountId, UUID.fromString(callerId));

        // Scope to member user IDs of this account
        List<UUID> memberIds = operatorAccountService.listMembers(accountId).stream()
                .filter(m -> m.getStatus() == OperatorMemberStatus.ACTIVE)
                .map(OperatorMembership::getUserId)
                .toList();

        // If a specific userId filter is requested, ensure it's a member of this account
        UUID effectiveUserId = (userId != null && memberIds.contains(userId)) ? userId : null;

        var page = auditLogRepository.findWithFilters(eventType, effectiveUserId, from, to, pageable)
                .map(this::toResponse);

        return ResponseEntity.ok(page);
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
