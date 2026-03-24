package io.k48.fortyeightid.admin.internal;

import io.k48.fortyeightid.auth.ApiKeyManagementPort;
import java.time.Instant;

record ApiKeyCreationResponse(
        String key,
        String applicationName,
        Instant createdAt) {

    static ApiKeyCreationResponse from(ApiKeyManagementPort.ApiKeyCreationResult result) {
        return new ApiKeyCreationResponse(
                result.rawKey(),
                result.apiKey().getAppName(),
                result.apiKey().getCreatedAt());
    }
}
