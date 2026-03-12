package io.k48.fortyeightid.auth.internal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.k48.fortyeightid.TestcontainersConfiguration;
import io.k48.fortyeightid.identity.Role;
import io.k48.fortyeightid.identity.User;
import io.k48.fortyeightid.identity.UserStatus;
import io.k48.fortyeightid.identity.internal.RoleRepository;
import io.k48.fortyeightid.identity.internal.UserRepository;
import java.time.Instant;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(TestcontainersConfiguration.class)
@Transactional
class AuthControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private PasswordResetTokenRepository passwordResetTokenRepository;

    private Role studentRole;

    @BeforeEach
    void setUp() {
        studentRole = roleRepository.findByName("STUDENT")
                .orElseGet(() -> {
                    var role = new Role();
                    role.setName("STUDENT");
                    return roleRepository.save(role);
                });
    }

    @Test
    void loginWithValidCredentials_returnsTokens() throws Exception {
        var user = createActiveUser("K48-2024-TEST-001", "test@k48.io", "SecurePass123!");

        var loginRequest = new LoginRequest("K48-2024-TEST-001", "SecurePass123!");

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.access_token").isNotEmpty())
                .andExpect(jsonPath("$.refresh_token").isNotEmpty())
                .andExpect(jsonPath("$.user.matricule").value("K48-2024-TEST-001"))
                .andExpect(jsonPath("$.requires_password_change").value(false));
    }

    @Test
    void loginWithPendingActivation_returns401() throws Exception {
        var user = createPendingUser("K48-2024-TEST-002", "pending@k48.io", "TempPass123!");

        var loginRequest = new LoginRequest("K48-2024-TEST-002", "TempPass123!");

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("ACCOUNT_NOT_ACTIVATED"));
    }

    @Test
    void activateAccount_transitionsToActive() throws Exception {
        var user = createPendingUser("K48-2024-TEST-003", "activate@k48.io", "TempPass123!");
        var token = createActivationToken(user);

        var activateRequest = new ActivateAccountRequest(token);

        mockMvc.perform(post("/api/v1/auth/activate-account")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(activateRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("activated")));

        var updatedUser = userRepository.findById(user.getId()).orElseThrow();
        assertThat(updatedUser.getStatus()).isEqualTo(UserStatus.ACTIVE);
    }

    @Test
    void activateAccount_withExpiredToken_returns400() throws Exception {
        var user = createPendingUser("K48-2024-TEST-004", "expired@k48.io", "TempPass123!");
        var expiredToken = createExpiredActivationToken(user);

        var activateRequest = new ActivateAccountRequest(expiredToken);

        mockMvc.perform(post("/api/v1/auth/activate-account")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(activateRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("RESET_TOKEN_EXPIRED"));
    }

    @Test
    void changePassword_updatesPasswordHash() throws Exception {
        var user = createActiveUser("K48-2024-TEST-005", "change@k48.io", "OldPass123!");
        var accessToken = createAccessTokenForUser(user);

        var changeRequest = new ChangePasswordRequest("OldPass123!", "NewSecure#2026");

        mockMvc.perform(post("/api/v1/auth/change-password")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(changeRequest)))
                .andExpect(status().isOk());

        var updatedUser = userRepository.findById(user.getId()).orElseThrow();
        assertThat(passwordEncoder.matches("NewSecure#2026", updatedUser.getPasswordHash())).isTrue();
    }

    @Test
    void forgotPassword_alwaysReturns200() throws Exception {
        var forgotRequest = new ForgotPasswordRequest("nonexistent@k48.io");

        mockMvc.perform(post("/api/v1/auth/forgot-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(forgotRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").isNotEmpty());
    }

    @Test
    void resetPassword_withValidToken_updatesPassword() throws Exception {
        var user = createActiveUser("K48-2024-TEST-006", "reset@k48.io", "OldPass123!");
        var resetToken = createPasswordResetToken(user);

        var resetRequest = new ResetPasswordRequest(resetToken, "NewSecure#2026");

        mockMvc.perform(post("/api/v1/auth/reset-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(resetRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("successful")));

        var updatedUser = userRepository.findById(user.getId()).orElseThrow();
        assertThat(passwordEncoder.matches("NewSecure#2026", updatedUser.getPasswordHash())).isTrue();
    }

    @Test
    void refresh_rotatesTokens() throws Exception {
        var user = createActiveUser("K48-2024-TEST-007", "refresh@k48.io", "SecurePass123!");
        var loginRequest = new LoginRequest("K48-2024-TEST-007", "SecurePass123!");

        var loginResponse = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andReturn();

        var loginBody = objectMapper.readValue(loginResponse.getResponse().getContentAsString(), LoginResponse.class);
        var refreshRequest = new RefreshRequest(loginBody.refreshToken());

        mockMvc.perform(post("/api/v1/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(refreshRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.access_token").isNotEmpty())
                .andExpect(jsonPath("$.refresh_token").isNotEmpty())
                .andExpect(jsonPath("$.refresh_token").value(org.hamcrest.Matchers.not(loginBody.refreshToken())));
    }

    private User createActiveUser(String matricule, String email, String password) {
        var user = User.builder()
                .matricule(matricule)
                .email(email)
                .name("Test User")
                .passwordHash(passwordEncoder.encode(password))
                .phone("+237600000000")
                .batch("2024")
                .specialization("SE")
                .status(UserStatus.ACTIVE)
                .roles(Set.of(studentRole))
                .profileCompleted(true)
                .requiresPasswordChange(false)
                .build();
        return userRepository.save(user);
    }

    private User createPendingUser(String matricule, String email, String password) {
        var user = User.builder()
                .matricule(matricule)
                .email(email)
                .name("Pending User")
                .passwordHash(passwordEncoder.encode(password))
                .phone("+237600000000")
                .batch("2024")
                .specialization("SE")
                .status(UserStatus.PENDING_ACTIVATION)
                .roles(Set.of(studentRole))
                .profileCompleted(false)
                .requiresPasswordChange(true)
                .build();
        return userRepository.save(user);
    }

    private String createActivationToken(User user) {
        var token = java.util.UUID.randomUUID().toString();
        var resetToken = PasswordResetToken.builder()
                .userId(user.getId())
                .token(token)
                .purpose(ResetTokenPurpose.ACCOUNT_ACTIVATION)
                .expiresAt(Instant.now().plusSeconds(86400))
                .used(false)
                .build();
        passwordResetTokenRepository.save(resetToken);
        return token;
    }

    private String createExpiredActivationToken(User user) {
        var token = java.util.UUID.randomUUID().toString();
        var resetToken = PasswordResetToken.builder()
                .userId(user.getId())
                .token(token)
                .purpose(ResetTokenPurpose.ACCOUNT_ACTIVATION)
                .expiresAt(Instant.now().minusSeconds(3600))
                .used(false)
                .build();
        passwordResetTokenRepository.save(resetToken);
        return token;
    }

    private String createPasswordResetToken(User user) {
        var token = java.util.UUID.randomUUID().toString();
        var resetToken = PasswordResetToken.builder()
                .userId(user.getId())
                .token(token)
                .purpose(ResetTokenPurpose.PASSWORD_RESET)
                .expiresAt(Instant.now().plusSeconds(3600))
                .used(false)
                .build();
        passwordResetTokenRepository.save(resetToken);
        return token;
    }

    private String createAccessTokenForUser(User user) throws Exception {
        var loginRequest = new LoginRequest(user.getMatricule(), "OldPass123!");
        var loginResponse = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andReturn();

        var loginBody = objectMapper.readValue(loginResponse.getResponse().getContentAsString(), LoginResponse.class);
        return loginBody.accessToken();
    }
}
