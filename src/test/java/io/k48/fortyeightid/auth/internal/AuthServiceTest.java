package io.k48.fortyeightid.auth.internal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.k48.fortyeightid.audit.AuditService;
import io.k48.fortyeightid.auth.PasswordPolicyService;
import io.k48.fortyeightid.identity.Role;
import io.k48.fortyeightid.identity.User;
import io.k48.fortyeightid.identity.UserQueryService;
import io.k48.fortyeightid.identity.UserStatus;
import io.k48.fortyeightid.identity.internal.UserRepository;
import io.k48.fortyeightid.shared.exception.PasswordPolicyViolationException;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserQueryService userQueryService;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtTokenService jwtTokenService;

    @Mock
    private RefreshTokenService refreshTokenService;

    @Mock
    private JwtConfig jwtConfig;

    @Mock
    private AuditService auditService;

    @Mock
    private PasswordPolicyService passwordPolicyService;

    @InjectMocks
    private AuthService authService;

    @Test
    void changePassword_successfullyChangesPassword() {
        var userId = UUID.randomUUID();
        var user = createUser(userId, "oldHash");

        when(userQueryService.findById(userId)).thenReturn(Optional.of(user));
        when(userQueryService.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(passwordEncoder.matches("OldPass@123", "oldHash")).thenReturn(true);
        when(passwordEncoder.encode("NewPass@456")).thenReturn("newHash");

        var request = new ChangePasswordRequest("OldPass@123", "NewPass@456");
        authService.changePassword(userId, request);

        assertThat(user.getPasswordHash()).isEqualTo("newHash");
        assertThat(user.isRequiresPasswordChange()).isFalse();
        verify(refreshTokenService, times(1)).revokeAllForUser(userId);
        verify(auditService, times(1)).log(eq(userId), eq("PASSWORD_CHANGED"), any());
    }

    @Test
    void changePassword_throwsWhenCurrentPasswordIsWrong() {
        var userId = UUID.randomUUID();
        var user = createUser(userId, "oldHash");

        when(userQueryService.findById(userId)).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("WrongPass@123", "oldHash")).thenReturn(false);

        var request = new ChangePasswordRequest("WrongPass@123", "NewPass@456");

        assertThatThrownBy(() -> authService.changePassword(userId, request))
                .isInstanceOf(BadCredentialsException.class)
                .hasMessage("Current password is incorrect.");
    }

    @Test
    void changePassword_throwsWhenNewPasswordTooShort() {
        var userId = UUID.randomUUID();
        var user = createUser(userId, "oldHash");

        when(userQueryService.findById(userId)).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("OldPass@123", "oldHash")).thenReturn(true);

        var request = new ChangePasswordRequest("OldPass@123", "Short1!");
        doThrow(new PasswordPolicyViolationException(List.of("Password must be at least 8 characters long")))
                .when(passwordPolicyService).validate("Short1!");

        assertThatThrownBy(() -> authService.changePassword(userId, request))
                .isInstanceOf(PasswordPolicyViolationException.class);
    }

    @Test
    void changePassword_throwsWhenNewPasswordMissingUppercase() {
        var userId = UUID.randomUUID();
        var user = createUser(userId, "oldHash");

        when(userQueryService.findById(userId)).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("OldPass@123", "oldHash")).thenReturn(true);

        var request = new ChangePasswordRequest("OldPass@123", "lowercase1@");
        doThrow(new PasswordPolicyViolationException(List.of("Password must contain at least one uppercase letter (A-Z)")))
                .when(passwordPolicyService).validate("lowercase1@");

        assertThatThrownBy(() -> authService.changePassword(userId, request))
                .isInstanceOf(PasswordPolicyViolationException.class);
    }

    @Test
    void changePassword_throwsWhenNewPasswordMissingLowercase() {
        var userId = UUID.randomUUID();
        var user = createUser(userId, "oldHash");

        when(userQueryService.findById(userId)).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("OldPass@123", "oldHash")).thenReturn(true);

        var request = new ChangePasswordRequest("OldPass@123", "UPPERCASE1@");
        doThrow(new PasswordPolicyViolationException(List.of("Password must contain at least one lowercase letter (a-z)")))
                .when(passwordPolicyService).validate("UPPERCASE1@");

        assertThatThrownBy(() -> authService.changePassword(userId, request))
                .isInstanceOf(PasswordPolicyViolationException.class);
    }

    @Test
    void changePassword_throwsWhenNewPasswordMissingDigit() {
        var userId = UUID.randomUUID();
        var user = createUser(userId, "oldHash");

        when(userQueryService.findById(userId)).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("OldPass@123", "oldHash")).thenReturn(true);

        var request = new ChangePasswordRequest("OldPass@123", "NoDigit@Abc");
        doThrow(new PasswordPolicyViolationException(List.of("Password must contain at least one digit (0-9)")))
                .when(passwordPolicyService).validate("NoDigit@Abc");

        assertThatThrownBy(() -> authService.changePassword(userId, request))
                .isInstanceOf(PasswordPolicyViolationException.class);
    }

    @Test
    void changePassword_throwsWhenNewPasswordMissingSpecialChar() {
        var userId = UUID.randomUUID();
        var user = createUser(userId, "oldHash");

        when(userQueryService.findById(userId)).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("OldPass@123", "oldHash")).thenReturn(true);

        var request = new ChangePasswordRequest("OldPass@123", "NoSpecial1A");
        doThrow(new PasswordPolicyViolationException(List.of("Password must contain at least one special character (@$!%*?&)")))
                .when(passwordPolicyService).validate("NoSpecial1A");

        assertThatThrownBy(() -> authService.changePassword(userId, request))
                .isInstanceOf(PasswordPolicyViolationException.class);
    }

    @Test
    void changePassword_throwsWhenUserNotFound() {
        var userId = UUID.randomUUID();
        when(userQueryService.findById(userId)).thenReturn(Optional.empty());

        var request = new ChangePasswordRequest("OldPass@123", "NewPass@456");

        assertThatThrownBy(() -> authService.changePassword(userId, request))
                .isInstanceOf(io.k48.fortyeightid.shared.exception.UserNotFoundException.class);
    }

    @Test
    void changePassword_throwsWhenNewPasswordIsSameAsCurrent() {
        var userId = UUID.randomUUID();
        var user = createUser(userId, "oldHash");

        when(userQueryService.findById(userId)).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("SamePass@123", "oldHash")).thenReturn(true);
        when(passwordEncoder.matches("SamePass@123", "oldHash")).thenReturn(true);

        var request = new ChangePasswordRequest("SamePass@123", "SamePass@123");

        assertThatThrownBy(() -> authService.changePassword(userId, request))
                .isInstanceOf(io.k48.fortyeightid.shared.exception.NewPasswordSameAsCurrentException.class);
    }

    private User createUser(UUID id, String passwordHash) {
        var role = new Role();
        role.setName("STUDENT");

        return User.builder()
                .id(id)
                .matricule("K48-2024-001")
                .email("test@k48.io")
                .name("Test User")
                .passwordHash(passwordHash)
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
