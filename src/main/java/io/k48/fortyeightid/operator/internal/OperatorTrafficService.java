package io.k48.fortyeightid.operator.internal;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.k48.fortyeightid.audit.AuditLog;
import io.k48.fortyeightid.audit.AuditLogRepository;
import io.k48.fortyeightid.identity.UserQueryService;
import io.k48.fortyeightid.shared.exception.OperatorAccountNotFoundException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
class OperatorTrafficService {

    private final OperatorAccountRepository operatorAccountRepository;
    private final OperatorMembershipRepository operatorMembershipRepository;
    private final AuditLogRepository auditLogRepository;
    private final UserQueryService userQueryService;
    private final ObjectMapper objectMapper;

    OperatorTrafficResponse getTrafficForOperator(UUID userId) {
        // Resolve account via membership
        var membership = operatorMembershipRepository
                .findByOperatorAccountIdAndUserIdAndStatus(null, userId, OperatorMemberStatus.ACTIVE)
                .or(() -> operatorMembershipRepository.findAllByUserId(userId).stream()
                        .filter(m -> m.getStatus() == OperatorMemberStatus.ACTIVE)
                        .findFirst())
                .orElseThrow(() -> new AccessDeniedException("User is not an active member of any operator account"));

        var account = operatorAccountRepository.findById(membership.getOperatorAccountId())
                .orElseThrow(() -> new OperatorAccountNotFoundException(
                        "Operator account not found: " + membership.getOperatorAccountId()));

        // API key calls
        List<OperatorTrafficResponse.ApiKeyCall> apiKeyCalls = List.of();
        if (account.getOwnedApiKeyId() != null) {
            var rawCalls = auditLogRepository.findApiKeyUsageByKeyId(account.getOwnedApiKeyId().toString());
            apiKeyCalls = rawCalls.stream().map(a -> {
                long totalInWindow = countInSameHourBucket(rawCalls, a.getCreatedAt());
                return new OperatorTrafficResponse.ApiKeyCall(
                        a.getCreatedAt(), a.getIpAddress(),
                        extractField(a, "endpoint"), extractField(a, "method"),
                        totalInWindow);
            }).toList();
        }

        // Member actions — all members of this account
        var memberIds = operatorMembershipRepository.findAllByOperatorAccountId(account.getId())
                .stream().map(OperatorMembership::getUserId).toList();

        var matriculeByUserId = memberIds.stream().collect(Collectors.toMap(
                Function.identity(),
                id -> userQueryService.findById(id).map(u -> u.getMatricule()).orElse("unknown")));

        List<OperatorTrafficResponse.MemberAction> memberActions = List.of();
        if (!memberIds.isEmpty()) {
            memberActions = auditLogRepository.findOperatorActionsByUserIds(memberIds).stream()
                    .map(a -> new OperatorTrafficResponse.MemberAction(
                            a.getUserId(),
                            matriculeByUserId.getOrDefault(a.getUserId(), "unknown"),
                            a.getAction(), extractField(a, "endpoint"), a.getCreatedAt()))
                    .toList();
        }

        return new OperatorTrafficResponse(apiKeyCalls, memberActions, Instant.now());
    }

    /** Count events in the same 1-hour bucket as the given timestamp. */
    private long countInSameHourBucket(List<AuditLog> calls, Instant timestamp) {
        var bucketStart = timestamp.truncatedTo(ChronoUnit.HOURS);
        var bucketEnd = bucketStart.plus(1, ChronoUnit.HOURS);
        return calls.stream()
                .filter(a -> !a.getCreatedAt().isBefore(bucketStart) && a.getCreatedAt().isBefore(bucketEnd))
                .count();
    }

    private String extractField(AuditLog log, String field) {
        try {
            var node = objectMapper.readTree(log.getDetails());
            var value = node.get(field);
            return value != null ? value.asText() : null;
        } catch (Exception e) {
            return null;
        }
    }
}
