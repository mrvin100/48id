package io.k48.fortyeightid.auth.internal;

import java.time.Instant;

record ApiKeyCreationResponse(
        String key,
        String applicationName,
        Instant createdAt) {

    static ApiKeyCreationResponse from(ApiKeyService.ApiKeyCreationResult result) {
        return new ApiKeyCreationResponse(
                result.rawKey(),
                result.apiKey().getAppName(),
                result.apiKey().getCreatedAt());
    }
}
