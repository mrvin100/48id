package io.k48.fortyeightid.operator.internal;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.k48.fortyeightid.admin.TrafficQueryPort;
import io.k48.fortyeightid.audit.AuditLog;
import io.k48.fortyeightid.audit.AuditLogRepository;
import io.k48.fortyeightid.identity.UserQueryService;
import io.k48.fortyeightid.shared.exception.OperatorAccountNotFoundException;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
class AdminTrafficService implements TrafficQueryPort {

    private static final long SECONDS_24H = 24 * 60 * 60;

    private final OperatorAccountRepository operatorAccountRepository;
    private final OperatorMembershipRepository operatorMembershipRepository;
    private final AuditLogRepository auditLogRepository;
    private final UserQueryService userQueryService;
    private final ObjectMapper objectMapper;

    // ── BE-11: aggregated traffic per account ─────────────────────────────────

    @Override
    public AggregatedTrafficView getAggregatedTraffic() {
        var accounts = operatorAccountRepository.findAll();
        var cutoff = Instant.now().minusSeconds(SECONDS_24H);

        var accountTraffics = accounts.stream().map(account -> {
            var apiKeyTraffic = buildApiKeyTraffic(account, cutoff);
            var memberActivity = buildMemberActivity(account, cutoff);
            return new AccountTraffic(account.getId(), account.getName(), apiKeyTraffic, memberActivity);
        }).toList();

        return new AggregatedTrafficView(accountTraffics, Instant.now());
    }

    private ApiKeyTraffic buildApiKeyTraffic(OperatorAccount account, Instant cutoff) {
        if (account.getOwnedApiKeyId() == null) {
            return new ApiKeyTraffic(0, 0, null);
        }
        var calls = auditLogRepository.findApiKeyUsageByKeyId(account.getOwnedApiKeyId().toString());
        long last24h = calls.stream().filter(a -> a.getCreatedAt().isAfter(cutoff)).count();
        var lastCalledAt = calls.isEmpty() ? null : calls.get(0).getCreatedAt();
        return new ApiKeyTraffic(calls.size(), last24h, lastCalledAt);
    }

    private MemberActivity buildMemberActivity(OperatorAccount account, Instant cutoff) {
        var memberIds = operatorMembershipRepository.findAllByOperatorAccountId(account.getId())
                .stream().map(OperatorMembership::getUserId).toList();
        if (memberIds.isEmpty()) {
            return new MemberActivity(0, 0, null);
        }
        var actions = auditLogRepository.findOperatorActionsByUserIds(memberIds);
        long last24h = actions.stream().filter(a -> a.getCreatedAt().isAfter(cutoff)).count();
        var lastActionAt = actions.isEmpty() ? null : actions.get(0).getCreatedAt();
        return new MemberActivity(actions.size(), last24h, lastActionAt);
    }

    // ── BE-12: detailed traffic for one account ───────────────────────────────

    @Override
    public AccountTrafficView getAccountTrafficDetail(UUID accountId, Pageable pageable) {
        var account = operatorAccountRepository.findById(accountId)
                .orElseThrow(() -> new OperatorAccountNotFoundException("Operator account not found: " + accountId));

        List<ApiKeyCall> apiKeyCalls = List.of();
        if (account.getOwnedApiKeyId() != null) {
            apiKeyCalls = auditLogRepository
                    .findApiKeyUsageByKeyIdPaged(account.getOwnedApiKeyId().toString(), pageable)
                    .map(a -> new ApiKeyCall(a.getCreatedAt(), a.getIpAddress(),
                            extractField(a, "endpoint"), extractField(a, "method")))
                    .getContent();
        }

        var memberIds = operatorMembershipRepository.findAllByOperatorAccountId(accountId)
                .stream().map(OperatorMembership::getUserId).toList();

        var matriculeByUserId = memberIds.stream().collect(Collectors.toMap(
                Function.identity(),
                id -> userQueryService.findById(id).map(u -> u.getMatricule()).orElse("unknown")));

        List<MemberAction> memberActions = List.of();
        if (!memberIds.isEmpty()) {
            memberActions = auditLogRepository
                    .findOperatorActionsByUserIdsPaged(memberIds, pageable)
                    .map(a -> new MemberAction(a.getUserId(),
                            matriculeByUserId.getOrDefault(a.getUserId(), "unknown"),
                            a.getAction(), extractField(a, "endpoint"), a.getCreatedAt()))
                    .getContent();
        }

        return new AccountTrafficView(account.getId(), account.getName(), apiKeyCalls, memberActions);
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
