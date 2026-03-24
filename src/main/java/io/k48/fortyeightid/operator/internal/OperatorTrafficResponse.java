package io.k48.fortyeightid.operator.internal;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

record OperatorTrafficResponse(
        List<ApiKeyCall> apiKeyCalls,
        List<MemberAction> memberActions,
        Instant generatedAt) {

    record ApiKeyCall(Instant timestamp, String ip, String endpoint, String method, long totalInWindow) {}

    record MemberAction(UUID userId, String matricule, String action, String endpoint, Instant timestamp) {}
}
