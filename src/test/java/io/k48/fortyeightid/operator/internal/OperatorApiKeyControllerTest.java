package io.k48.fortyeightid.operator.internal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.k48.fortyeightid.auth.ApiKey;
import io.k48.fortyeightid.shared.exception.OperatorOwnershipRequiredException;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class OperatorApiKeyControllerTest {

    @Mock private OperatorAccountService operatorAccountService;
    @InjectMocks private OperatorApiKeyController controller;

    @Test
    void createApiKey_ownerReturns201WithRawKey() {
        var accountId = UUID.randomUUID();
        var userId = UUID.randomUUID();
        var apiKey = ApiKey.builder().id(UUID.randomUUID()).appName("MyApp").keyHash("h").createdAt(Instant.now()).build();
        var result = new OperatorApiKeyCreationResult("raw-key", apiKey);
        var request = new CreateOperatorApiKeyRequest("MyApp", "desc");

        when(operatorAccountService.createApiKey(eq(accountId), eq(userId), anyString(), anyString()))
                .thenReturn(result);

        var response = controller.createApiKey(request, accountId, userId.toString());

        assertThat(response.getStatusCode().value()).isEqualTo(201);
        assertThat(response.getBody().key()).isEqualTo("raw-key");
        assertThat(response.getBody().applicationName()).isEqualTo("MyApp");
    }

    @Test
    void createApiKey_collaboratorThrowsForbidden() {
        var accountId = UUID.randomUUID();
        var userId = UUID.randomUUID();
        var request = new CreateOperatorApiKeyRequest("MyApp", "desc");

        doThrow(new OperatorOwnershipRequiredException("Only the OWNER can perform this action"))
                .when(operatorAccountService).createApiKey(any(), any(), any(), any());

        assertThatThrownBy(() -> controller.createApiKey(request, accountId, userId.toString()))
                .isInstanceOf(OperatorOwnershipRequiredException.class);
    }

    @Test
    void getApiKey_returnsViewForMember() {
        var accountId = UUID.randomUUID();
        var userId = UUID.randomUUID();
        var view = new OperatorApiKeyView(UUID.randomUUID(), "MyApp", "desc", Instant.now(), null);

        when(operatorAccountService.getApiKey(accountId, userId)).thenReturn(view);

        var response = controller.getApiKey(accountId, userId.toString());

        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(response.getBody().appName()).isEqualTo("MyApp");
    }

    @Test
    void getApiKey_returnsNoContentWhenNoKeyLinked() {
        var accountId = UUID.randomUUID();
        var userId = UUID.randomUUID();

        when(operatorAccountService.getApiKey(accountId, userId)).thenReturn(null);

        var response = controller.getApiKey(accountId, userId.toString());

        assertThat(response.getStatusCode().value()).isEqualTo(204);
    }

    @Test
    void rotateApiKey_ownerReturns200WithNewKey() {
        var accountId = UUID.randomUUID();
        var userId = UUID.randomUUID();
        var result = new OperatorApiKeyRotationResult("new-raw-key", "MyApp", Instant.now());

        when(operatorAccountService.rotateApiKey(accountId, userId)).thenReturn(result);

        var response = controller.rotateApiKey(accountId, userId.toString());

        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(response.getBody().key()).isEqualTo("new-raw-key");
    }

    @Test
    void deleteApiKey_ownerReturns204() {
        var accountId = UUID.randomUUID();
        var userId = UUID.randomUUID();

        var response = controller.deleteApiKey(accountId, userId.toString());

        assertThat(response.getStatusCode().value()).isEqualTo(204);
        verify(operatorAccountService).deleteApiKey(accountId, userId);
    }

    @Test
    void deleteApiKey_collaboratorThrowsForbidden() {
        var accountId = UUID.randomUUID();
        var userId = UUID.randomUUID();

        doThrow(new OperatorOwnershipRequiredException("Only the OWNER can perform this action"))
                .when(operatorAccountService).deleteApiKey(any(), any());

        assertThatThrownBy(() -> controller.deleteApiKey(accountId, userId.toString()))
                .isInstanceOf(OperatorOwnershipRequiredException.class);
    }
}
