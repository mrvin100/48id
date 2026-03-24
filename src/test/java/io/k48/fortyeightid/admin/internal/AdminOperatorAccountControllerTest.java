package io.k48.fortyeightid.admin.internal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.k48.fortyeightid.audit.AuditService;
import io.k48.fortyeightid.operator.OperatorAccountPort;
import io.k48.fortyeightid.operator.OperatorAccountPort.CreateOperatorAccountCommand;
import io.k48.fortyeightid.operator.OperatorAccountPort.OperatorAccountCreated;
import io.k48.fortyeightid.shared.exception.OperatorAccountNameTakenException;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AdminOperatorAccountControllerTest {

    @Mock private OperatorAccountPort operatorAccountPort;
    @Mock private AuditService auditService;
    @InjectMocks private AdminOperatorAccountController controller;

    private final UUID adminId = UUID.randomUUID();
    private final UUID accountId = UUID.randomUUID();
    private final UUID membershipId = UUID.randomUUID();

    private OperatorAccountCreated sampleCreated() {
        return new OperatorAccountCreated(
            accountId, "48Hub Team", "Main hub", adminId, Instant.now(),
            membershipId, adminId, "K48-B1-01", "Ulrich"
        );
    }

    @Test
    void createAccount_shouldReturn201_whenValid() {
        var request = new CreateOperatorAccountRequest("48Hub Team", "Main hub");
        when(operatorAccountPort.createAccount(any())).thenReturn(sampleCreated());

        var response = controller.createAccount(request, adminId.toString());

        assertThat(response.getStatusCode().value()).isEqualTo(201);
        var body = response.getBody();
        assertThat(body).isNotNull();
        assertThat(body.id()).isEqualTo(accountId);
        assertThat(body.name()).isEqualTo("48Hub Team");
        assertThat(body.owner().matricule()).isEqualTo("K48-B1-01");
        assertThat(body.owner().name()).isEqualTo("Ulrich");
    }

    @Test
    void createAccount_shouldPassAdminUuidFromPrincipal() {
        var request = new CreateOperatorAccountRequest("48Hub Team", null);
        when(operatorAccountPort.createAccount(any())).thenReturn(sampleCreated());

        controller.createAccount(request, adminId.toString());

        verify(operatorAccountPort).createAccount(
            eq(new CreateOperatorAccountCommand("48Hub Team", null, adminId)));
    }

    @Test
    void createAccount_shouldEmitAuditEvent_whenSuccessful() {
        var request = new CreateOperatorAccountRequest("48Hub Team", "desc");
        when(operatorAccountPort.createAccount(any())).thenReturn(sampleCreated());

        controller.createAccount(request, adminId.toString());

        verify(auditService).log(eq(adminId), eq("OPERATOR_ACCOUNT_CREATED"), any());
    }

    @Test
    void createAccount_shouldPropagate_whenNameTaken() {
        var request = new CreateOperatorAccountRequest("48Hub Team", null);
        when(operatorAccountPort.createAccount(any()))
            .thenThrow(new OperatorAccountNameTakenException("48Hub Team"));

        assertThatThrownBy(() -> controller.createAccount(request, adminId.toString()))
            .isInstanceOf(OperatorAccountNameTakenException.class);
    }
}
