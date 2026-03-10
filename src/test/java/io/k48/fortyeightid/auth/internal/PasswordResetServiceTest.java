package io.k48.fortyeightid.auth.internal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.k48.fortyeightid.audit.AuditService;
import io.k48.fortyeightid.auth.EmailPort;
import io.k48.fortyeightid.identity.Role;
import io.k48.fortyeightid.identity.User;
import io.k48.fortyeightid.identity.UserQueryService;
import io.k48.fortyeightid.identity.UserStatus;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PasswordResetServiceTest {

    @Mock
    private PasswordResetTokenRepository passwordResetTokenRepository;

    @Mock
    private EmailPort emailService;

    @Mock
    private UserQueryService userQueryService;

    @Mock
    private AuditService auditService;

    @InjectMocks
    private PasswordResetService passwordResetService;

    @Test
    void handleForgotPassword_sendsEmailWhenUserExists() {
        var user = createUser("test@k48.io");
        when(userQueryService.findByEmail("test@k48.io")).thenReturn(Optional.of(user));

        passwordResetService.handleForgotPassword("test@k48.io");

        verify(emailService, times(1)).sendPasswordResetEmail(eq("test@k48.io"), any(), any());
        verify(passwordResetTokenRepository, times(1)).deleteAllByUserId(user.getId());
        verify(passwordResetTokenRepository, times(1)).save(any(PasswordResetToken.class));
        verify(auditService, times(1)).log(eq(user.getId()), eq("PASSWORD_RESET_REQUESTED"), any());
    }

    @Test
    void handleForgotPassword_doesNotSendEmailWhenUserDoesNotExist() {
        when(userQueryService.findByEmail("nonexistent@k48.io")).thenReturn(Optional.empty());

        passwordResetService.handleForgotPassword("nonexistent@k48.io");

        verify(emailService, never()).sendPasswordResetEmail(any(), any(), any());
        verify(passwordResetTokenRepository, never()).deleteAllByUserId(any());
        verify(passwordResetTokenRepository, never()).save(any());
        verify(auditService, never()).log(any(), any(), any());
    }

    @Test
    void handleForgotPassword_returnsSameResponseForExistingAndNonExistingEmail() {
        var user = createUser("test@k48.io");
        when(userQueryService.findByEmail("test@k48.io")).thenReturn(Optional.of(user));
        when(userQueryService.findByEmail("nonexistent@k48.io")).thenReturn(Optional.empty());

        passwordResetService.handleForgotPassword("test@k48.io");
        passwordResetService.handleForgotPassword("nonexistent@k48.io");

        verify(emailService, times(1)).sendPasswordResetEmail(any(), any(), any());
        verify(auditService, times(1)).log(any(), any(), any());
    }

    @Test
    void handleForgotPassword_deletesExistingTokensBeforeCreatingNew() {
        var user = createUser("test@k48.io");
        when(userQueryService.findByEmail("test@k48.io")).thenReturn(Optional.of(user));

        passwordResetService.handleForgotPassword("test@k48.io");

        verify(passwordResetTokenRepository, times(1)).deleteAllByUserId(user.getId());
        verify(passwordResetTokenRepository, times(1)).save(any(PasswordResetToken.class));
    }

    private User createUser(String email) {
        var role = new Role();
        role.setName("STUDENT");

        return User.builder()
                .id(UUID.randomUUID())
                .matricule("K48-2024-001")
                .email(email)
                .name("Test User")
                .passwordHash("hash")
                .status(UserStatus.ACTIVE)
                .batch("2024")
                .specialization("SE")
                .phone("+237600000000")
                .profileCompleted(false)
                .requiresPasswordChange(false)
                .roles(Set.of(role))
                .build();
    }
}
