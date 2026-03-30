package io.k48.fortyeightid.operator.internal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;

@ExtendWith(MockitoExtension.class)
class OperatorUserControllerTest {

    @Mock
    private OperatorApiConsumerService operatorApiConsumerService;
    @Mock
    private OperatorAccountService operatorAccountService;

    @InjectMocks
    private OperatorUserController operatorUserController;

    @Test
    void listApiConsumers_returnsPaginatedConsumers() {
        // Given: API consumers exist for the account
        var accountId = UUID.randomUUID();
        var userId = UUID.randomUUID();
        var callerId = userId.toString();
        var consumer = buildConsumer(UUID.randomUUID(), "K48-B1-1");
        Page<OperatorApiConsumerService.ApiConsumerSummary> page = new PageImpl<>(List.of(consumer));

        doNothing().when(operatorAccountService).requireActiveMember(eq(accountId), eq(userId));
        when(operatorApiConsumerService.listApiConsumers(eq(accountId), any(Pageable.class))).thenReturn(page);

        // When: Operator lists API consumers
        ResponseEntity<Page<OperatorUserController.ApiConsumerResponse>> response =
                operatorUserController.listApiConsumers(accountId, PageRequest.of(0, 20), callerId);

        // Then: Returns paginated consumers with 200 OK
        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getContent()).hasSize(1);
        assertThat(response.getBody().getContent().get(0).matricule()).isEqualTo("K48-B1-1");
        verify(operatorApiConsumerService, times(1)).listApiConsumers(eq(accountId), any(Pageable.class));
    }

    @Test
    void listApiConsumers_verifiesActiveMembership() {
        // Given
        var accountId = UUID.randomUUID();
        var userId = UUID.randomUUID();
        var callerId = userId.toString();
        Page<OperatorApiConsumerService.ApiConsumerSummary> emptyPage = new PageImpl<>(List.of());

        doNothing().when(operatorAccountService).requireActiveMember(eq(accountId), eq(userId));
        when(operatorApiConsumerService.listApiConsumers(eq(accountId), any(Pageable.class))).thenReturn(emptyPage);

        // When
        operatorUserController.listApiConsumers(accountId, PageRequest.of(0, 20), callerId);

        // Then: membership check was called
        verify(operatorAccountService, times(1)).requireActiveMember(accountId, userId);
    }

    private OperatorApiConsumerService.ApiConsumerSummary buildConsumer(UUID userId, String matricule) {
        return new OperatorApiConsumerService.ApiConsumerSummary(
                userId, matricule, matricule + "@k48.io", "Test User",
                "B1", "ACTIVE", 42L, Instant.now(), Instant.now()
        );
    }
}
