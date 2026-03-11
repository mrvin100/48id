package io.k48.fortyeightid.auth.internal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import io.k48.fortyeightid.identity.Role;
import io.k48.fortyeightid.identity.User;
import io.k48.fortyeightid.identity.UserQueryService;
import io.k48.fortyeightid.identity.UserStatus;
import io.k48.fortyeightid.shared.exception.JwtSignatureException;
import io.k48.fortyeightid.shared.exception.JwtTokenExpiredException;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.jwt.Jwt;

@ExtendWith(MockitoExtension.class)
class IdentityControllerTest {

    @Mock
    private JwtTokenService jwtTokenService;

    @Mock
    private UserQueryService userQueryService;

    @InjectMocks
    private IdentityController identityController;

    @Test
    void verifyToken_validJwt_returnsValidResponse() {
        var userId = UUID.randomUUID();
        var jwt = createMockJwt(userId.toString());
        var user = createUser(userId, UserStatus.ACTIVE);

        when(jwtTokenService.validateToken("valid-jwt")).thenReturn(jwt);
        when(userQueryService.findById(userId)).thenReturn(Optional.of(user));

        var request = new VerifyTokenRequest("valid-jwt");
        var response = identityController.verifyToken(request);

        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getBody().valid()).isTrue();
        assertThat(response.getBody().user()).isNotNull();
        assertThat(response.getBody().user().matricule()).isEqualTo("K48-2024-001");
    }

    @Test
    void verifyToken_expiredJwt_returnsInvalidResponse() {
        when(jwtTokenService.validateToken("expired-jwt"))
                .thenThrow(new JwtTokenExpiredException("Token expired"));

        var request = new VerifyTokenRequest("expired-jwt");
        var response = identityController.verifyToken(request);

        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getBody().valid()).isFalse();
        assertThat(response.getBody().reason()).isEqualTo("TOKEN_EXPIRED");
    }

    @Test
    void verifyToken_invalidJwt_returnsInvalidResponse() {
        when(jwtTokenService.validateToken("invalid-jwt"))
                .thenThrow(new JwtSignatureException("Invalid token"));

        var request = new VerifyTokenRequest("invalid-jwt");
        var response = identityController.verifyToken(request);

        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getBody().valid()).isFalse();
        assertThat(response.getBody().reason()).isEqualTo("TOKEN_INVALID");
    }

    @Test
    void verifyToken_suspendedUser_returnsInvalidResponse() {
        var userId = UUID.randomUUID();
        var jwt = createMockJwt(userId.toString());
        var user = createUser(userId, UserStatus.SUSPENDED);

        when(jwtTokenService.validateToken("valid-jwt")).thenReturn(jwt);
        when(userQueryService.findById(userId)).thenReturn(Optional.of(user));

        var request = new VerifyTokenRequest("valid-jwt");
        var response = identityController.verifyToken(request);

        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getBody().valid()).isFalse();
        assertThat(response.getBody().reason()).isEqualTo("ACCOUNT_SUSPENDED");
    }

    @Test
    void verifyToken_userNotFound_returnsInvalidResponse() {
        var userId = UUID.randomUUID();
        var jwt = createMockJwt(userId.toString());

        when(jwtTokenService.validateToken("valid-jwt")).thenReturn(jwt);
        when(userQueryService.findById(userId)).thenReturn(Optional.empty());

        var request = new VerifyTokenRequest("valid-jwt");
        var response = identityController.verifyToken(request);

        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getBody().valid()).isFalse();
        assertThat(response.getBody().reason()).isEqualTo("USER_NOT_FOUND");
    }

    private Jwt createMockJwt(String subject) {
        return Jwt.withTokenValue("token")
                .subject(subject)
                .claim("role", "STUDENT")
                .build();
    }

    private User createUser(UUID id, UserStatus status) {
        var role = new Role();
        role.setName("STUDENT");

        return User.builder()
                .id(id)
                .matricule("K48-2024-001")
                .email("test@k48.io")
                .name("Test User")
                .passwordHash("hash")
                .status(status)
                .batch("2024")
                .specialization("SE")
                .phone("+237600000000")
                .profileCompleted(true)
                .roles(Set.of(role))
                .build();
    }
}
