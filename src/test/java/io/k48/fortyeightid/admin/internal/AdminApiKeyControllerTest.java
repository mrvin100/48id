package io.k48.fortyeightid.admin.internal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.k48.fortyeightid.auth.ApiKey;
import io.k48.fortyeightid.auth.ApiKeyManagementPort;
import io.k48.fortyeightid.auth.ApiKeyManagementPort.ApiKeyCreationResult;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AdminApiKeyControllerTest {

    @Mock private ApiKeyManagementPort apiKeyService;
    @InjectMocks private AdminApiKeyController adminApiKeyController;

    @Test
    void createApiKey_returnsRawKeyAndMetadata() {
        var adminId = UUID.randomUUID();
        var request = new CreateApiKeyRequest("48Hub", "Alumni directory");

        var apiKey = ApiKey.builder()
                .id(UUID.randomUUID())
                .appName("48Hub")
                .description("Alumni directory")
                .keyHash("hash")
                .build();

        var result = new ApiKeyCreationResult("raw-key-abc123", apiKey);
        when(apiKeyService.createApiKey(eq("48Hub"), eq("Alumni directory"), any(UUID.class)))
                .thenReturn(result);

        var response = adminApiKeyController.createApiKey(request, adminId.toString());

        assertThat(response.getStatusCode().value()).isEqualTo(201);
        var body = response.getBody();
        assertThat(body).isNotNull();
        assertThat(body.key()).isEqualTo("raw-key-abc123");
        assertThat(body.applicationName()).isEqualTo("48Hub");
        verify(apiKeyService).createApiKey("48Hub", "Alumni directory", adminId);
    }

    @Test
    void listApiKeys_returnsMetadataOnly() {
        var apiKey1 = ApiKey.builder()
                .id(UUID.randomUUID())
                .appName("48Hub")
                .description("Alumni directory")
                .keyHash("hash1")
                .createdAt(Instant.now())
                .build();

        var apiKey2 = ApiKey.builder()
                .id(UUID.randomUUID())
                .appName("LP48")
                .keyHash("hash2")
                .createdAt(Instant.now())
                .lastUsedAt(Instant.now().minusSeconds(3600))
                .build();

        when(apiKeyService.listAll()).thenReturn(List.of(apiKey1, apiKey2));

        var response = adminApiKeyController.listApiKeys();

        assertThat(response.getStatusCode().value()).isEqualTo(200);
        var body = response.getBody();
        assertThat(body).hasSize(2);
        assertThat(body.get(0).applicationName()).isEqualTo("48Hub");
        assertThat(body.get(0).description()).isEqualTo("Alumni directory");
        assertThat(body.get(1).applicationName()).isEqualTo("LP48");
        assertThat(body.get(1).lastUsedAt()).isNotNull();
    }

    @Test
    void revokeApiKey_deletesKeyAndReturnsNoContent() {
        var apiKeyId = UUID.randomUUID();
        var adminId = UUID.randomUUID();

        var response = adminApiKeyController.revokeApiKey(apiKeyId, adminId.toString());

        assertThat(response.getStatusCode().value()).isEqualTo(204);
        verify(apiKeyService).revokeApiKey(apiKeyId, adminId);
    }
}
