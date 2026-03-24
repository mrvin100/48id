package io.k48.fortyeightid.operator.internal;

import jakarta.validation.Valid;
import java.time.Instant;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("${fortyeightid.api.prefix}/operator/api-keys")
@PreAuthorize("hasRole('OPERATOR')")
@RequiredArgsConstructor
class OperatorApiKeyController {

    private final OperatorAccountService operatorAccountService;

    @PostMapping
    ResponseEntity<ApiKeyCreatedResponse> createApiKey(
            @Valid @RequestBody CreateOperatorApiKeyRequest request,
            @RequestParam UUID accountId,
            @AuthenticationPrincipal String userId) {
        var result = operatorAccountService.createApiKey(
                accountId, UUID.fromString(userId), request.applicationName(), request.description());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new ApiKeyCreatedResponse(result.rawKey(), result.apiKey().getId(), result.apiKey().getAppName()));
    }

    @GetMapping
    ResponseEntity<OperatorApiKeyView> getApiKey(
            @RequestParam UUID accountId,
            @AuthenticationPrincipal String userId) {
        var view = operatorAccountService.getApiKey(accountId, UUID.fromString(userId));
        if (view == null) {
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.ok(view);
    }

    @PutMapping("/rotate")
    ResponseEntity<ApiKeyRotatedResponse> rotateApiKey(
            @RequestParam UUID accountId,
            @AuthenticationPrincipal String userId) {
        var result = operatorAccountService.rotateApiKey(accountId, UUID.fromString(userId));
        return ResponseEntity.ok(new ApiKeyRotatedResponse(result.rawKey(), result.applicationName(), result.rotatedAt()));
    }

    @DeleteMapping
    ResponseEntity<Void> deleteApiKey(
            @RequestParam UUID accountId,
            @AuthenticationPrincipal String userId) {
        operatorAccountService.deleteApiKey(accountId, UUID.fromString(userId));
        return ResponseEntity.noContent().build();
    }

    record ApiKeyCreatedResponse(String key, UUID id, String applicationName) {}

    record ApiKeyRotatedResponse(String key, String applicationName, Instant rotatedAt) {}
}
