package io.k48.fortyeightid.auth.internal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.k48.fortyeightid.identity.User;
import io.k48.fortyeightid.identity.UserQueryService;
import io.k48.fortyeightid.identity.UserStatus;
import io.k48.fortyeightid.shared.exception.RefreshTokenInvalidException;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock private UserQueryService userQueryService;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private JwtTokenService jwtTokenService;
    @Mock private RefreshTokenService refreshTokenService;

    private AuthService authService;

    @BeforeEach
    void setUp() {
        var config = new JwtConfig();
        config.setAccessTokenExpiry(900);
        authService = new AuthService(userQueryService, passwordEncoder, jwtTokenService, refreshTokenService, config);
    }

    private User activeUser() {
        return User.builder()
                .id(UUID.randomUUID())
                .matricule("K48-2024-001")
                .email("test@k48.io")
                .name("Test User")
                .passwordHash("$2a$10$hash")
                .status(UserStatus.ACTIVE)
                .batch("2024")
                .specialization("Software Engineering")
                .build();
    }

    @Test
    void login_returnsTokens() {
        var user = activeUser();
        when(userQueryService.findByMatricule("K48-2024-001")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("password", "$2a$10$hash")).thenReturn(true);
        when(jwtTokenService.generateAccessToken(any(), any())).thenReturn("jwt-token");
        when(refreshTokenService.createRefreshToken(any())).thenReturn("refresh-token");

        var response = authService.login(new LoginRequest("K48-2024-001", "password"));

        assertThat(response.accessToken()).isEqualTo("jwt-token");
        assertThat(response.refreshToken()).isEqualTo("refresh-token");
        assertThat(response.tokenType()).isEqualTo("Bearer");
        assertThat(response.expiresIn()).isEqualTo(900);
        assertThat(response.user().matricule()).isEqualTo("K48-2024-001");
    }

    @Test
    void login_throwsOnUnknownMatricule() {
        when(userQueryService.findByMatricule("NONE")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.login(new LoginRequest("NONE", "pass")))
                .isInstanceOf(BadCredentialsException.class)
                .hasMessage("Matricule or password is incorrect.");
    }

    @Test
    void login_throwsOnWrongPassword() {
        var user = activeUser();
        when(userQueryService.findByMatricule("K48-2024-001")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("wrong", "$2a$10$hash")).thenReturn(false);

        assertThatThrownBy(() -> authService.login(new LoginRequest("K48-2024-001", "wrong")))
                .isInstanceOf(BadCredentialsException.class);
    }

    @Test
    void login_throwsOnSuspendedAccount() {
        var user = activeUser();
        user.setStatus(UserStatus.SUSPENDED);
        when(userQueryService.findByMatricule("K48-2024-001")).thenReturn(Optional.of(user));

        assertThatThrownBy(() -> authService.login(new LoginRequest("K48-2024-001", "pass")))
                .isInstanceOf(DisabledException.class)
                .hasMessageContaining("suspended");
    }

    @Test
    void login_throwsOnPendingActivation() {
        var user = activeUser();
        user.setStatus(UserStatus.PENDING_ACTIVATION);
        when(userQueryService.findByMatricule("K48-2024-001")).thenReturn(Optional.of(user));

        assertThatThrownBy(() -> authService.login(new LoginRequest("K48-2024-001", "pass")))
                .isInstanceOf(DisabledException.class)
                .hasMessageContaining("activate");
    }

    @Test
    void refresh_returnsNewTokens() {
        var user = activeUser();
        var result = new RefreshTokenService.RefreshTokenResult(user.getId(), "new-refresh");
        when(refreshTokenService.validateAndRotate("old-refresh")).thenReturn(result);
        when(userQueryService.findById(user.getId())).thenReturn(Optional.of(user));
        when(jwtTokenService.generateAccessToken(any(), any())).thenReturn("new-jwt");

        var response = authService.refresh(new RefreshRequest("old-refresh"));

        assertThat(response.accessToken()).isEqualTo("new-jwt");
        assertThat(response.refreshToken()).isEqualTo("new-refresh");
        assertThat(response.tokenType()).isEqualTo("Bearer");
    }

    @Test
    void refresh_throwsOnSuspendedUser() {
        var user = activeUser();
        user.setStatus(UserStatus.SUSPENDED);
        var result = new RefreshTokenService.RefreshTokenResult(user.getId(), "new-refresh");
        when(refreshTokenService.validateAndRotate("token")).thenReturn(result);
        when(userQueryService.findById(user.getId())).thenReturn(Optional.of(user));

        assertThatThrownBy(() -> authService.refresh(new RefreshRequest("token")))
                .isInstanceOf(DisabledException.class)
                .hasMessageContaining("suspended");
    }

    @Test
    void refresh_throwsOnInvalidToken() {
        when(refreshTokenService.validateAndRotate("bad")).thenThrow(new RefreshTokenInvalidException("invalid"));

        assertThatThrownBy(() -> authService.refresh(new RefreshRequest("bad")))
                .isInstanceOf(RefreshTokenInvalidException.class);
    }

    @Test
    void logout_revokesRefreshToken() {
        doNothing().when(refreshTokenService).revokeToken("some-token");

        authService.logout(new LogoutRequest("some-token"));

        verify(refreshTokenService).revokeToken("some-token");
    }
}
