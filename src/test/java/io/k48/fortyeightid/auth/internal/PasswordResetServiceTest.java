package io.k48.fortyeightid.auth.internal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
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
import io.k48.fortyeightid.shared.exception.PasswordPolicyViolationException;
import io.k48.fortyeightid.shared.exception.ResetTokenExpiredException;
import io.k48.fortyeightid.shared.exception.ResetTokenInvalidException;
import java.time.Instant;
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
class PasswordResetServiceTest {

    @Mock
    private PasswordResetTokenRepository passwordResetTokenRepository;

    @Mock
    private EmailPort emailService;

    @Mock
    private UserQueryService userQueryService;

    @Mock
    private AuditService auditService;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private RefreshTokenService refreshTokenService;

    @InjectMocks
    private PasswordResetService passwordResetService;

    @Test
    void resetPassword_successfullyResetsPassword() {
        var token = UUID.randomUUID().toString();
        var userId = UUID.randomUUID();
        var resetToken = createResetToken(token, userId, false);
        var user = createUser(userId);

        when(passwordResetTokenRepository.findByToken(token)).thenReturn(Optional.of(resetToken));
        when(userQueryService.findById(userId)).thenReturn(Optional.of(user));
        when(userQueryService.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(passwordEncoder.encode("NewSecure@123")).thenReturn("newHash");

        passwordResetService.resetPassword(token, "NewSecure@123");

        assertThat(user.getPasswordHash()).isEqualTo("newHash");
        assertThat(user.isRequiresPasswordChange()).isFalse();
        assertThat(resetToken.isUsed()).isTrue();
        verify(refreshTokenService, times(1)).revokeAllForUser(userId);
        verify(auditService, times(1)).log(eq(userId), eq("PASSWORD_RESET_COMPLETED"), any());
    }

    @Test
    void resetPassword_throwsWhenTokenIsInvalid() {
        when(passwordResetTokenRepository.findByToken("invalid-token")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> passwordResetService.resetPassword("invalid-token", "NewSecure@123"))
                .isInstanceOf(ResetTokenInvalidException.class)
                .hasMessage("Invalid reset token.");
    }

    @Test
    void resetPassword_throwsWhenTokenIsExpired() {
        var token = UUID.randomUUID().toString();
        var userId = UUID.randomUUID();
        var resetToken = createResetToken(token, userId, true);

        when(passwordResetTokenRepository.findByToken(token)).thenReturn(Optional.of(resetToken));

        assertThatThrownBy(() -> passwordResetService.resetPassword(token, "NewSecure@123"))
                .isInstanceOf(ResetTokenExpiredException.class)
                .hasMessage("This reset link has expired. Please request a new one.");
    }

    @Test
    void resetPassword_throwsWhenTokenIsAlreadyUsed() {
        var token = UUID.randomUUID().toString();
        var userId = UUID.randomUUID();
        var resetToken = createResetToken(token, userId, false);
        resetToken.setUsed(true);

        when(passwordResetTokenRepository.findByToken(token)).thenReturn(Optional.of(resetToken));

        assertThatThrownBy(() -> passwordResetService.resetPassword(token, "NewSecure@123"))
                .isInstanceOf(ResetTokenInvalidException.class)
                .hasMessage("Invalid reset token.");
    }

    @Test
    void resetPassword_throwsWhenPasswordDoesNotMeetPolicy() {
        var token = UUID.randomUUID().toString();
        var userId = UUID.randomUUID();
        var resetToken = createResetToken(token, userId, false);

        when(passwordResetTokenRepository.findByToken(token)).thenReturn(Optional.of(resetToken));

        assertThatThrownBy(() -> passwordResetService.resetPassword(token, "weak"))
                .isInstanceOf(PasswordPolicyViolationException.class);

        verify(passwordResetTokenRepository, never()).delete(any());
        verify(auditService, never()).log(any(), any(), any());
    }

    @Test
    void resetPassword_throwsWhenUserNotFound() {
        var token = UUID.randomUUID().toString();
        var userId = UUID.randomUUID();
        var resetToken = createResetToken(token, userId, false);

        when(passwordResetTokenRepository.findByToken(token)).thenReturn(Optional.of(resetToken));
        when(userQueryService.findById(userId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> passwordResetService.resetPassword(token, "NewSecure@123"))
                .isInstanceOf(io.k48.fortyeightid.shared.exception.UserNotFoundException.class);
    }

    private PasswordResetToken createResetToken(String token, UUID userId, boolean expired) {
        var expiresAt = expired ? Instant.now().minusSeconds(3600) : Instant.now().plusSeconds(3600);
        
        return PasswordResetToken.builder()
                .userId(userId)
                .token(token)
                .expiresAt(expiresAt)
                .used(false)
                .build();
    }

    private User createUser(UUID id) {
        var role = new Role();
        role.setName("STUDENT");

        return User.builder()
                .id(id)
                .matricule("K48-2024-001")
                .email("test@k48.io")
                .name("Test User")
                .passwordHash("oldHash")
                .status(UserStatus.ACTIVE)
                .batch("2024")
                .specialization("SE")
                .phone("+237600000000")
                .profileCompleted(false)
                .requiresPasswordChange(true)
                .roles(Set.of(role))
                .build();
    }
}
