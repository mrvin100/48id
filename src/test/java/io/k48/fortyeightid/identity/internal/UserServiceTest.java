package io.k48.fortyeightid.identity.internal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.k48.fortyeightid.audit.AuditService;
import io.k48.fortyeightid.identity.Role;
import io.k48.fortyeightid.identity.User;
import io.k48.fortyeightid.identity.UserStatus;
import io.k48.fortyeightid.shared.exception.UserNotFoundException;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private AuditService auditService;

    @InjectMocks
    private UserService userService;

    @Test
    void updateProfile_updatesPhoneAndSpecialization() {
        var userId = UUID.randomUUID();
        var user = createUser(userId, "K48-2024-001", false);

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var request = new UpdateProfileRequest("+237600000001", "Data Science");
        var updated = userService.updateProfile(userId, request);

        assertThat(updated.getPhone()).isEqualTo("+237600000001");
        assertThat(updated.getSpecialization()).isEqualTo("Data Science");
        verify(auditService, times(1)).log(eq(userId), eq("PROFILE_UPDATED"), any(Map.class));
    }

    @Test
    void updateProfile_setsProfileCompletedToTrue() {
        var userId = UUID.randomUUID();
        var user = createUser(userId, "K48-2024-001", false);
        user.setName("John Doe");

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var request = new UpdateProfileRequest("+237600000001", null);
        var updated = userService.updateProfile(userId, request);

        assertThat(updated.isProfileCompleted()).isTrue();
    }

    @Test
    void updateProfile_doesNotChangeProfileCompletedIfAlreadyTrue() {
        var userId = UUID.randomUUID();
        var user = createUser(userId, "K48-2024-001", true);

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var request = new UpdateProfileRequest("+237600000001", "Data Science");
        var updated = userService.updateProfile(userId, request);

        assertThat(updated.isProfileCompleted()).isTrue();
    }

    @Test
    void updateProfile_handlesNullPhoneGracefully() {
        var userId = UUID.randomUUID();
        var user = createUser(userId, "K48-2024-001", false);

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var request = new UpdateProfileRequest(null, "Data Science");
        var updated = userService.updateProfile(userId, request);

        assertThat(updated.getPhone()).isEqualTo("+237600000000");
        assertThat(updated.getSpecialization()).isEqualTo("Data Science");
    }

    @Test
    void updateProfile_handlesNullSpecializationGracefully() {
        var userId = UUID.randomUUID();
        var user = createUser(userId, "K48-2024-001", false);

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var request = new UpdateProfileRequest("+237600000001", null);
        var updated = userService.updateProfile(userId, request);

        assertThat(updated.getPhone()).isEqualTo("+237600000001");
        assertThat(updated.getSpecialization()).isEqualTo("Software Engineering");
    }

    @Test
    void updateProfile_throwsWhenUserNotFound() {
        var userId = UUID.randomUUID();
        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        var request = new UpdateProfileRequest("+237600000001", "Data Science");

        assertThatThrownBy(() -> userService.updateProfile(userId, request))
                .isInstanceOf(UserNotFoundException.class);
    }

    @Test
    void updateProfile_doesNotSetProfileCompletedWithoutName() {
        var userId = UUID.randomUUID();
        var user = createUser(userId, "K48-2024-001", false);
        user.setName(null);

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var request = new UpdateProfileRequest("+237600000001", "Data Science");
        var updated = userService.updateProfile(userId, request);

        assertThat(updated.isProfileCompleted()).isFalse();
    }

    @Test
    void updateProfile_doesNotSetProfileCompletedWithBlankName() {
        var userId = UUID.randomUUID();
        var user = createUser(userId, "K48-2024-001", false);
        user.setName("  ");

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var request = new UpdateProfileRequest("+237600000001", "Data Science");
        var updated = userService.updateProfile(userId, request);

        assertThat(updated.isProfileCompleted()).isFalse();
    }

    private User createUser(UUID id, String matricule, boolean profileCompleted) {
        var role = new Role();
        role.setName("STUDENT");

        return User.builder()
                .id(id)
                .matricule(matricule)
                .email("test@k48.io")
                .name("Test User")
                .passwordHash("hash")
                .status(UserStatus.ACTIVE)
                .batch("2024")
                .specialization("Software Engineering")
                .phone("+237600000000")
                .profileCompleted(profileCompleted)
                .roles(Set.of(role))
                .build();
    }
}
