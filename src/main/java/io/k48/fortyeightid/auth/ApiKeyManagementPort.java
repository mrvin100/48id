package io.k48.fortyeightid.auth;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Public port for API key management operations.
 * Implemented by auth module internals.
 */
public interface ApiKeyManagementPort {

    Optional<ApiKey> validate(String rawKey);

    ApiKeyCreationResult createApiKey(String appName, String description, UUID createdBy);

    List<ApiKey> listAll();

    void revokeApiKey(UUID apiKeyId, UUID revokedBy);

    void updateLastUsed(ApiKey apiKey);

    ApiKeyRotationResult rotateApiKey(UUID apiKeyId, UUID rotatedBy);

    record ApiKeyCreationResult(String rawKey, ApiKey apiKey) {}

    record ApiKeyRotationResult(String rawKey, String applicationName, Instant rotatedAt) {}
}
