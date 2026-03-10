package io.k48.fortyeightid.auth.internal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HexFormat;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ApiKeyServiceTest {

    @Mock private ApiKeyRepository apiKeyRepository;
    @Mock private io.k48.fortyeightid.audit.AuditService auditService;
    @InjectMocks private ApiKeyService apiKeyService;

    private String sha256(String input) throws Exception {
        var digest = MessageDigest.getInstance("SHA-256");
        return HexFormat.of().formatHex(digest.digest(input.getBytes(StandardCharsets.UTF_8)));
    }

    @Test
    void validate_returnsKeyForValidActiveKey() throws Exception {
        var rawKey = "test-api-key-123";
        var hash = sha256(rawKey);
        var apiKey = ApiKey.builder()
                .id(UUID.randomUUID())
                .appName("TestApp")
                .keyHash(hash)
                .active(true)
                .build();
        when(apiKeyRepository.findByKeyHash(hash)).thenReturn(Optional.of(apiKey));

        var result = apiKeyService.validate(rawKey);

        assertThat(result).isPresent();
        assertThat(result.get().getAppName()).isEqualTo("TestApp");
    }

    @Test
    void validate_returnsEmptyForInactiveKey() throws Exception {
        var rawKey = "test-api-key-123";
        var hash = sha256(rawKey);
        var apiKey = ApiKey.builder()
                .id(UUID.randomUUID())
                .appName("TestApp")
                .keyHash(hash)
                .active(false)
                .build();
        when(apiKeyRepository.findByKeyHash(hash)).thenReturn(Optional.of(apiKey));

        assertThat(apiKeyService.validate(rawKey)).isEmpty();
    }

    @Test
    void validate_returnsEmptyForExpiredKey() throws Exception {
        var rawKey = "test-api-key-123";
        var hash = sha256(rawKey);
        var apiKey = ApiKey.builder()
                .id(UUID.randomUUID())
                .appName("TestApp")
                .keyHash(hash)
                .active(true)
                .expiresAt(Instant.now().minus(1, ChronoUnit.DAYS))
                .build();
        when(apiKeyRepository.findByKeyHash(hash)).thenReturn(Optional.of(apiKey));

        assertThat(apiKeyService.validate(rawKey)).isEmpty();
    }

    @Test
    void validate_returnsEmptyForUnknownKey() {
        when(apiKeyRepository.findByKeyHash(anyString())).thenReturn(Optional.empty());

        assertThat(apiKeyService.validate("unknown-key")).isEmpty();
    }
}
