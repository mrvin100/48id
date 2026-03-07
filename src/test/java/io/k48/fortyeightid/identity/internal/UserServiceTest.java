package io.k48.fortyeightid.identity.internal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import io.k48.fortyeightid.identity.User;
import io.k48.fortyeightid.identity.UserStatus;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.k48.fortyeightid.shared.exception.DuplicateEmailException;
import io.k48.fortyeightid.shared.exception.DuplicateMatriculeException;
import io.k48.fortyeightid.shared.exception.UserNotFoundException;
import java.util.Optional;
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

    @InjectMocks
    private UserService userService;

    private CreateUserRequest validRequest() {
        return new CreateUserRequest(
                "K48-2024-001", "test@k48.io", "Test User",
                null, "2024", null, "password123");
    }

    @Test
    void createUser_savesWithBcryptHash() {
        when(userRepository.existsByMatricule(anyString())).thenReturn(false);
        when(userRepository.existsByEmail(anyString())).thenReturn(false);
        when(passwordEncoder.encode("password123")).thenReturn("$2a$10$hashed");
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        var user = userService.createUser(validRequest());

        assertThat(user.getPasswordHash()).isEqualTo("$2a$10$hashed");
        assertThat(user.getStatus()).isEqualTo(UserStatus.PENDING_ACTIVATION);
        assertThat(user.isProfileCompleted()).isFalse();
    }

    @Test
    void createUser_throwsOnDuplicateMatricule() {
        when(userRepository.existsByMatricule("K48-2024-001")).thenReturn(true);

        assertThatThrownBy(() -> userService.createUser(validRequest()))
                .isInstanceOf(DuplicateMatriculeException.class);
        verify(userRepository, never()).save(any());
    }

    @Test
    void createUser_throwsOnDuplicateEmail() {
        when(userRepository.existsByMatricule(anyString())).thenReturn(false);
        when(userRepository.existsByEmail("test@k48.io")).thenReturn(true);

        assertThatThrownBy(() -> userService.createUser(validRequest()))
                .isInstanceOf(DuplicateEmailException.class);
        verify(userRepository, never()).save(any());
    }

    @Test
    void findById_throwsWhenNotFound() {
        var id = UUID.randomUUID();
        when(userRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.findById(id))
                .isInstanceOf(UserNotFoundException.class);
    }

    @Test
    void updateStatus_changesStatus() {
        var id = UUID.randomUUID();
        var user = User.builder().matricule("K48-2024-001").email("t@k48.io")
                .name("T").passwordHash("h").status(UserStatus.ACTIVE).build();
        when(userRepository.findById(id)).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        var result = userService.updateStatus(id, UserStatus.SUSPENDED);

        assertThat(result.getStatus()).isEqualTo(UserStatus.SUSPENDED);
    }
}
