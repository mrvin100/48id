package io.k48.fortyeightid.admin.internal;

import io.k48.fortyeightid.auth.ApiKeyManagementPort;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("${fortyeightid.api.prefix}/admin/api-keys")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
class AdminApiKeyController {

    private final ApiKeyManagementPort apiKeyService;

    @PostMapping
    ResponseEntity<ApiKeyCreationResponse> createApiKey(
            @Valid @RequestBody CreateApiKeyRequest request,
            @AuthenticationPrincipal String adminId) {
        var result = apiKeyService.createApiKey(
                request.applicationName(),
                request.description(),
                UUID.fromString(adminId));
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiKeyCreationResponse.from(result));
    }

    @GetMapping
    ResponseEntity<List<ApiKeyResponse>> listApiKeys() {
        var apiKeys = apiKeyService.listAll().stream()
                .map(ApiKeyResponse::from)
                .toList();
        return ResponseEntity.ok(apiKeys);
    }

    @DeleteMapping("/{id}")
    ResponseEntity<Void> revokeApiKey(@PathVariable UUID id,
                                      @AuthenticationPrincipal String adminId) {
        apiKeyService.revokeApiKey(id, UUID.fromString(adminId));
        return ResponseEntity.noContent().build();
    }
}
