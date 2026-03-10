package io.k48.fortyeightid.identity.internal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import io.k48.fortyeightid.identity.UserQueryService;
import io.k48.fortyeightid.identity.Role;
import io.k48.fortyeightid.identity.User;
import io.k48.fortyeightid.identity.UserStatus;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;

import static org.mockito.ArgumentMatchers.eq;

@ExtendWith(MockitoExtension.class)
class MeControllerTest {

    @Mock
    private UserService userService;

    @Mock
    private UserQueryService userQueryService;

    @InjectMocks
    private MeController meController;

    @Test
    void updateProfile_returnsUpdatedProfile() {
        var userId = UUID.randomUUID();
        var updatedUser = createUser(userId, true);

        var request = new UpdateProfileRequest("+237600000001", "Data Science");
        when(userService.updateProfile(eq(userId), any(UpdateProfileRequest.class))).thenReturn(updatedUser);

        ResponseEntity<MeResponse> response = meController.updateProfile(userId.toString(), request);

        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getBody().phone()).isEqualTo("+237600000001");
        assertThat(response.getBody().specialization()).isEqualTo("Data Science");
        assertThat(response.getBody().profileCompleted()).isTrue();
    }

    @Test
    void updateProfile_returnsProfileCompletedFalseWhenNotComplete() {
        var userId = UUID.randomUUID();
        var updatedUser = createUser(userId, false);

        var request = new UpdateProfileRequest("+237600000001", null);
        when(userService.updateProfile(eq(userId), any(UpdateProfileRequest.class))).thenReturn(updatedUser);

        ResponseEntity<MeResponse> response = meController.updateProfile(userId.toString(), request);

        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getBody().profileCompleted()).isFalse();
    }

    private User createUser(UUID id, boolean profileCompleted) {
        var role = new Role();
        role.setName("STUDENT");

        return User.builder()
                .id(id)
                .matricule("K48-2024-001")
                .email("test@k48.io")
                .name("Test User")
                .passwordHash("hash")
                .status(UserStatus.ACTIVE)
                .batch("2024")
                .specialization("Data Science")
                .phone("+237600000001")
                .profileCompleted(profileCompleted)
                .roles(Set.of(role))
                .build();
    }
}
