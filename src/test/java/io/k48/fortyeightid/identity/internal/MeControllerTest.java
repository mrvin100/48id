package io.k48.fortyeightid.identity.internal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import io.k48.fortyeightid.identity.User;
import io.k48.fortyeightid.identity.UserQueryService;
import io.k48.fortyeightid.identity.UserStatus;
import io.k48.fortyeightid.shared.exception.UserNotFoundException;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class MeControllerTest {

    @Mock private UserQueryService userQueryService;
    @InjectMocks private MeController meController;

    @Test
    void me_returnsAuthenticatedUserData() {
        var userId = UUID.randomUUID();
        var user = User.builder()
                .id(userId)
                .matricule("K48-2024-001")
                .email("test@k48.io")
                .name("Test User")
                .passwordHash("hash")
                .status(UserStatus.ACTIVE)
                .batch("2024")
                .specialization("SE")
                .build();
        when(userQueryService.findById(userId)).thenReturn(Optional.of(user));

        var response = meController.me(userId.toString());

        assertThat(response.getStatusCode().value()).isEqualTo(200);
        var body = response.getBody();
        assertThat(body.matricule()).isEqualTo("K48-2024-001");
        assertThat(body.email()).isEqualTo("test@k48.io");
        assertThat(body.name()).isEqualTo("Test User");
        assertThat(body.batch()).isEqualTo("2024");
    }

    @Test
    void me_throwsWhenUserNotFound() {
        var userId = UUID.randomUUID();
        when(userQueryService.findById(userId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> meController.me(userId.toString()))
                .isInstanceOf(UserNotFoundException.class);
    }
}
