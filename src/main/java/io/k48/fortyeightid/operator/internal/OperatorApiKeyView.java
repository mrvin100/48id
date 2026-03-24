package io.k48.fortyeightid.operator.internal;

import io.k48.fortyeightid.auth.ApiKey;
import java.time.Instant;
import java.util.UUID;

record OperatorApiKeyView(
        UUID id,
        String appName,
        String description,
        Instant createdAt,
        Instant lastUsedAt
) {
    static OperatorApiKeyView from(ApiKey apiKey) {
        return new OperatorApiKeyView(
                apiKey.getId(),
                apiKey.getAppName(),
                apiKey.getDescription(),
                apiKey.getCreatedAt(),
                apiKey.getLastUsedAt()
        );
    }
}
