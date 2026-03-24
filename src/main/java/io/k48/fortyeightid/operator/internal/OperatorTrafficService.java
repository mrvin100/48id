package io.k48.fortyeightid.operator.internal;

import io.k48.fortyeightid.audit.AuditLogRepository;
import io.k48.fortyeightid.audit.AuditLogUtils;
import io.k48.fortyeightid.identity.UserQueryService;
import io.k48.fortyeightid.shared.exception.OperatorAccountNotFoundException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
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

    OperatorTrafficResponse getTrafficForOperator(UUID userId) {
        var membership = operatorMembershipRepository.findAllByUserId(userId).stream()
                .filter(m -> m.getStatus() == OperatorMemberStatus.ACTIVE)
                .findFirst()
                .orElseThrow(() -> new AccessDeniedException(
                        "User is not an active member of any operator account"));

        var account = operatorAccountRepository.findById(membership.getOperatorAccountId())
                .orElseThrow(() -> new OperatorAccountNotFoundException(
                        "Operator account not found: " + membership.getOperatorAccountId()));

        // API key calls — pre-compute hour-bucket counts in a single pass
        List<OperatorTrafficResponse.ApiKeyCall> apiKeyCalls = List.of();
        if (account.getOwnedApiKeyId() != null) {
            var rawCalls = auditLogRepository.findApiKeyUsageByKeyId(account.getOwnedApiKeyId().toString());

            // Single pass: group by truncated hour → count
            Map<Instant, Long> countByHour = rawCalls.stream()
                    .collect(Collectors.groupingBy(
                            a -> a.getCreatedAt().truncatedTo(ChronoUnit.HOURS),
                            Collectors.counting()));

            apiKeyCalls = rawCalls.stream().map(a -> new OperatorTrafficResponse.ApiKeyCall(
                    a.getCreatedAt(), a.getIpAddress(),
                    AuditLogUtils.extractField(a, "endpoint"),
                    AuditLogUtils.extractField(a, "method"),
                    countByHour.getOrDefault(a.getCreatedAt().truncatedTo(ChronoUnit.HOURS), 0L)))
                    .toList();
        }

        // Member actions
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
                            a.getAction(), AuditLogUtils.extractField(a, "endpoint"), a.getCreatedAt()))
                    .toList();
        }

        return new OperatorTrafficResponse(apiKeyCalls, memberActions, Instant.now());
    }
}
