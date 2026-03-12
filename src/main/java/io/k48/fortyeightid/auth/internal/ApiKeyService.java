package io.k48.fortyeightid.auth.internal;

import io.k48.fortyeightid.audit.AuditService;
import io.k48.fortyeightid.auth.ApiKey;
import io.k48.fortyeightid.auth.ApiKeyManagementPort;
import io.k48.fortyeightid.shared.exception.ApiKeyNotFoundException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
class ApiKeyService implements ApiKeyManagementPort {

    private final ApiKeyRepository apiKeyRepository;
    private final AuditService auditService;
    private final SecureRandom secureRandom = new SecureRandom();

    @Override
    public Optional<ApiKey> validate(String rawKey) {
        var hash = sha256(rawKey);
        return apiKeyRepository.findByKeyHash(hash)
                .filter(ApiKey::isActive)
                .filter(key -> key.getExpiresAt() == null || key.getExpiresAt().isAfter(Instant.now()));
    }

    @Override
    @Transactional
    public ApiKeyCreationResult createApiKey(String appName, String description, UUID createdBy) {
        var rawKey = generateSecureKey();
        var hash = sha256(rawKey);

        var apiKey = ApiKey.builder()
                .appName(appName)
                .description(description)
                .keyHash(hash)
                .createdBy(createdBy)
                .build();

        var saved = apiKeyRepository.save(apiKey);

        auditService.log(createdBy, "API_KEY_CREATED", Map.of(
                "apiKeyId", saved.getId().toString(),
                "appName", appName
        ));

        return new ApiKeyCreationResult(rawKey, saved);
    }

    @Override
    public List<ApiKey> listAll() {
        return apiKeyRepository.findAll();
    }

    @Override
    @Transactional
    public void revokeApiKey(UUID apiKeyId, UUID revokedBy) {
        var apiKey = apiKeyRepository.findById(apiKeyId)
                .orElseThrow(() -> new ApiKeyNotFoundException("API key not found: " + apiKeyId));

        apiKeyRepository.deleteById(apiKeyId);

        auditService.log(revokedBy, "API_KEY_REVOKED", Map.of(
                "apiKeyId", apiKeyId.toString(),
                "appName", apiKey.getAppName()
        ));
    }

    @Override
    @Transactional
    public void updateLastUsed(ApiKey apiKey) {
        apiKey.updateLastUsed();
        apiKeyRepository.save(apiKey);
    }

    @Override
    @Transactional
    public ApiKeyRotationResult rotateApiKey(UUID apiKeyId, UUID rotatedBy) {
        var apiKey = apiKeyRepository.findById(apiKeyId)
                .orElseThrow(() -> new ApiKeyNotFoundException("API key not found: " + apiKeyId));

        // Generate new key
        var rawKey = generateSecureKey();
        var newHash = sha256(rawKey);

        // Update the key hash
        apiKey.setKeyHash(newHash);
        apiKeyRepository.save(apiKey);

        // Log audit event
        auditService.log(rotatedBy, "API_KEY_ROTATED", Map.of(
                "apiKeyId", apiKeyId.toString(),
                "appName", apiKey.getAppName()
        ));

        return new ApiKeyRotationResult(rawKey, apiKey.getAppName(), Instant.now());
    }

    private String generateSecureKey() {
        var bytes = new byte[32]; // 256 bits
        secureRandom.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private String sha256(String input) {
        try {
            var digest = MessageDigest.getInstance("SHA-256");
            var hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }
}
