package io.k48.fortyeightid.operator.internal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

import io.k48.fortyeightid.shared.exception.ResetTokenExpiredException;
import io.k48.fortyeightid.shared.exception.ResetTokenInvalidException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class OperatorInviteControllerTest {

    @Mock private OperatorInviteService operatorInviteService;
    @InjectMocks private OperatorInviteController controller;

    @Test
    void acceptOperatorInvite_validRequest_returnsSuccess() {
        var request = new AcceptOperatorInviteRequest("valid-token");

        var response = controller.acceptOperatorInvite(request);

        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(response.getBody()).containsEntry("success", true);
        verify(operatorInviteService).acceptInviteFlow("valid-token");
    }

    @Test
    void acceptOperatorInvite_invalidToken_throws() {
        var request = new AcceptOperatorInviteRequest("bad-token");
        doThrow(new ResetTokenInvalidException("Invalid invite token."))
                .when(operatorInviteService).acceptInviteFlow("bad-token");

        assertThatThrownBy(() -> controller.acceptOperatorInvite(request))
                .isInstanceOf(ResetTokenInvalidException.class);
    }

    @Test
    void acceptOperatorInvite_expiredToken_throws() {
        var request = new AcceptOperatorInviteRequest("expired-token");
        doThrow(new ResetTokenExpiredException("This invite link has expired."))
                .when(operatorInviteService).acceptInviteFlow("expired-token");

        assertThatThrownBy(() -> controller.acceptOperatorInvite(request))
                .isInstanceOf(ResetTokenExpiredException.class);
    }
}
