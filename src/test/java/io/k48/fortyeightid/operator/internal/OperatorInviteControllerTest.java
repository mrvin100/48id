package io.k48.fortyeightid.operator.internal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.k48.fortyeightid.audit.AuditService;
import io.k48.fortyeightid.auth.OperatorInviteTokenPort;
import io.k48.fortyeightid.identity.UserRoleService;
import io.k48.fortyeightid.shared.exception.ResetTokenExpiredException;
import io.k48.fortyeightid.shared.exception.ResetTokenInvalidException;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class OperatorInviteControllerTest {

    @Mock private OperatorInviteTokenPort operatorInviteTokenPort;
    @Mock private OperatorAccountService operatorAccountService;
    @Mock private UserRoleService userRoleService;
    @Mock private AuditService auditService;

    @InjectMocks private OperatorInviteController controller;

    @Test
    void acceptOperatorInvite_validToken_returnsSuccess() {
        var userId = UUID.randomUUID();
        var request = new AcceptOperatorInviteRequest("valid-token");

        when(operatorInviteTokenPort.validateAndConsumeInviteToken("valid-token")).thenReturn(userId);

        var response = controller.acceptOperatorInvite(request);

        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(response.getBody()).containsEntry("success", true);
        verify(operatorAccountService).acceptInvite(userId);
        verify(userRoleService).changeRole(userId, "OPERATOR");
        verify(auditService).log(userId, "OPERATOR_INVITE_ACCEPTED", java.util.Map.of("userId", userId.toString()));
    }

    @Test
    void acceptOperatorInvite_invalidToken_throws() {
        var request = new AcceptOperatorInviteRequest("bad-token");
        doThrow(new ResetTokenInvalidException("Invalid invite token."))
                .when(operatorInviteTokenPort).validateAndConsumeInviteToken("bad-token");

        assertThatThrownBy(() -> controller.acceptOperatorInvite(request))
                .isInstanceOf(ResetTokenInvalidException.class);
    }

    @Test
    void acceptOperatorInvite_expiredToken_throws() {
        var request = new AcceptOperatorInviteRequest("expired-token");
        doThrow(new ResetTokenExpiredException("This invite link has expired. Please request a new one."))
                .when(operatorInviteTokenPort).validateAndConsumeInviteToken("expired-token");

        assertThatThrownBy(() -> controller.acceptOperatorInvite(request))
                .isInstanceOf(ResetTokenExpiredException.class);
    }
}
