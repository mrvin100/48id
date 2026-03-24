package io.k48.fortyeightid.admin;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Pageable;

/**
 * Public port exposing traffic data for cross-module consumers (operator module).
 */
public interface TrafficQueryPort {

    AggregatedTrafficView getAggregatedTraffic();

    AccountTrafficView getAccountTrafficDetail(UUID accountId, Pageable pageable);

    // ── Aggregated view (BE-11) ───────────────────────────────────────────────

    record AggregatedTrafficView(List<AccountTraffic> accounts, Instant generatedAt) {}

    record AccountTraffic(UUID accountId, String accountName,
                          ApiKeyTraffic apiKeyTraffic, MemberActivity memberActivity) {}

    record ApiKeyTraffic(long totalCalls, long last24h, Instant lastCalledAt) {}

    record MemberActivity(long totalActions, long last24h, Instant lastActionAt) {}

    // ── Detail view (BE-12) ───────────────────────────────────────────────────

    record AccountTrafficView(UUID accountId, String accountName,
                              List<ApiKeyCall> apiKeyCalls, List<MemberAction> memberActions) {}

    record ApiKeyCall(Instant timestamp, String ip, String endpoint, String method) {}

    record MemberAction(UUID userId, String matricule, String action, String endpoint, Instant timestamp) {}
}
