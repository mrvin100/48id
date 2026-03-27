package io.k48.fortyeightid.operator.internal;

import io.k48.fortyeightid.audit.AuditLog;
import io.k48.fortyeightid.audit.AuditLogRepository;
import io.k48.fortyeightid.audit.AuditLogUtils;
import io.k48.fortyeightid.identity.UserQueryService;
import io.k48.fortyeightid.shared.exception.OperatorAccountNotFoundException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Resolves the list of 48ID users who have authenticated externally via a specific
 * operator account's API key (i.e., API consumers of that operator's platform).
 *
 * Data source: audit_log entries with action = 'API_KEY_USED' and details->>'keyId'
 * matching the account's owned API key ID.
 *
 * Each consumer record includes:
 * - Matricule, email, batch, status — from user profile
 * - Total API calls via this key — aggregated from audit log
 * - First seen / Last seen timestamps
 */
@Slf4j
@Service
@RequiredArgsConstructor
class OperatorApiConsumerService {

    private final OperatorAccountRepository operatorAccountRepository;
    private final AuditLogRepository auditLogRepository;
    private final UserQueryService userQueryService;

    record ApiConsumerSummary(
            UUID userId,
            String matricule,
            String email,
            String name,
            String batch,
            String status,
            long totalCalls,
            Instant firstSeen,
            Instant lastSeen
    ) {}

    @Transactional(readOnly = true)
    Page<ApiConsumerSummary> listApiConsumers(UUID accountId, Pageable pageable) {
        var account = operatorAccountRepository.findById(accountId)
                .orElseThrow(() -> new OperatorAccountNotFoundException(
                        "Operator account not found: " + accountId));

        if (account.getOwnedApiKeyId() == null) {
            return Page.empty(pageable);
        }

        // Fetch all audit log entries for this API key
        var allCalls = auditLogRepository
                .findApiKeyUsageByKeyId(account.getOwnedApiKeyId().toString());

        // Group by userId — track call count, first seen, last seen
        // LinkedHashMap preserves insertion order (audit log is DESC by created_at, so first entry = latest)
        Map<UUID, ConsumerAccumulator> accumulators = new LinkedHashMap<>();
        for (AuditLog call : allCalls) {
            var userId = call.getUserId();
            if (userId == null) continue; // anonymous/key-only calls have no userId

            accumulators.compute(userId, (id, acc) -> {
                if (acc == null) {
                    return new ConsumerAccumulator(call.getCreatedAt(), call.getCreatedAt(), 1);
                }
                return new ConsumerAccumulator(
                        // allCalls is DESC so first entry is latest, accumulate min for firstSeen
                        acc.lastSeen().isBefore(call.getCreatedAt()) ? call.getCreatedAt() : acc.lastSeen(),
                        acc.firstSeen().isAfter(call.getCreatedAt()) ? call.getCreatedAt() : acc.firstSeen(),
                        acc.totalCalls() + 1
                );
            });
        }

        // Build summaries — enrich with user data
        List<ApiConsumerSummary> summaries = new ArrayList<>();
        for (var entry : accumulators.entrySet()) {
            UUID userId = entry.getKey();
            ConsumerAccumulator acc = entry.getValue();
            userQueryService.findById(userId).ifPresent(user ->
                    summaries.add(new ApiConsumerSummary(
                            userId,
                            user.getMatricule(),
                            user.getEmail(),
                            user.getName(),
                            user.getBatch(),
                            user.getStatus().name(),
                            acc.totalCalls(),
                            acc.firstSeen(),
                            acc.lastSeen()
                    ))
            );
        }

        // Sort by lastSeen descending (most recently active first)
        summaries.sort(Comparator.comparing(ApiConsumerSummary::lastSeen).reversed());

        // Manual pagination
        int total = summaries.size();
        int from = (int) pageable.getOffset();
        int to = Math.min(from + pageable.getPageSize(), total);
        List<ApiConsumerSummary> page = (from >= total) ? List.of() : summaries.subList(from, to);

        return new PageImpl<>(page, pageable, total);
    }

    private record ConsumerAccumulator(Instant lastSeen, Instant firstSeen, long totalCalls) {}
}
