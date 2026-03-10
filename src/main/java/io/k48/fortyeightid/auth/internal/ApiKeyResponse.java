package io.k48.fortyeightid.auth.internal;

import java.time.Instant;
import java.util.UUID;

record ApiKeyResponse(
        UUID id,
        String applicationName,
        String description,
        Instant createdAt,
        Instant lastUsedAt) {

    static ApiKeyResponse from(ApiKey apiKey) {
        return new ApiKeyResponse(
                apiKey.getId(),
                apiKey.getAppName(),
                apiKey.getDescription(),
                apiKey.getCreatedAt(),
                apiKey.getLastUsedAt());
    }
}
