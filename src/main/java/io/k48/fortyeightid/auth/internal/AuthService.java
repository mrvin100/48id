package io.k48.fortyeightid.auth.internal;

import io.k48.fortyeightid.audit.AuditService;
import io.k48.fortyeightid.auth.PasswordPolicyService;
import io.k48.fortyeightid.identity.User;
import io.k48.fortyeightid.identity.UserQueryService;
import io.k48.fortyeightid.identity.UserStatus;
import io.k48.fortyeightid.shared.exception.AccountLockedException;
import io.k48.fortyeightid.shared.exception.NewPasswordSameAsCurrentException;
import io.k48.fortyeightid.shared.exception.PasswordPolicyViolationException;
import io.k48.fortyeightid.shared.exception.UserNotFoundException;
import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
class AuthService {

    private final UserQueryService userQueryService;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenService jwtTokenService;
    private final RefreshTokenService refreshTokenService;
    private final JwtConfig jwtConfig;
    private final AuditService auditService;
    private final LoginAttemptService loginAttemptService;
    private final PasswordPolicyService passwordPolicyService;

    @Transactional
    LoginResponse login(LoginRequest request) {
        // Check if account is locked
        if (loginAttemptService.isLocked(request.matricule())) {
            long remainingSeconds = loginAttemptService.getRemainingLockoutTime(request.matricule());
            throw new AccountLockedException(remainingSeconds);
        }

        var userOpt = userQueryService.findByMatricule(request.matricule());

        if (userOpt.isEmpty()) {
            loginAttemptService.recordFailedAttempt(request.matricule());
            throw new BadCredentialsException("Matricule or password is incorrect.");
        }

        var user = userOpt.get();

        if (user.getStatus() == UserStatus.SUSPENDED) {
            throw new DisabledException("Your account has been suspended. Contact K48 administration.");
        }

        if (user.getStatus() == UserStatus.PENDING_ACTIVATION) {
            throw new DisabledException("Please activate your account. Check your email for an activation link.");
        }

        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            boolean locked = loginAttemptService.recordFailedAttempt(request.matricule());
            if (locked) {
                auditService.log(user.getId(), "ACCOUNT_LOCKED", Map.of(
                        "userId", user.getId().toString(),
                        "matricule", request.matricule(),
                        "reason", "Too many failed login attempts"
                ));
            }
            throw new BadCredentialsException("Matricule or password is incorrect.");
        }

        // Successful login - reset failed attempts
        loginAttemptService.resetFailedAttempts(request.matricule());

        // Log successful login audit event
        auditService.log(user.getId(), "LOGIN_SUCCESS", Map.of(
                "userId", user.getId().toString(),
                "matricule", user.getMatricule()
        ));

        // Update last login timestamp
        user.setLastLoginAt(OffsetDateTime.now());
        userQueryService.save(user);

        var principal = new UserPrincipal(user);
        var accessToken = jwtTokenService.generateAccessToken(principal, user);
        var refreshToken = refreshTokenService.createRefreshToken(user.getId());

        var roles = user.getRoles().stream()
                .map(r -> r.getName())
                .toList();

        var userInfo = new LoginResponse.UserInfo(
                user.getId().toString(),
                user.getMatricule(),
                user.getName(),
                roles,
                user.getBatch(),
                user.getSpecialization()
        );

        return new LoginResponse(
                accessToken,
                refreshToken,
                "Bearer",
                jwtConfig.getAccessTokenExpiry(),
                user.isRequiresPasswordChange(),
                userInfo
        );
    }

    RefreshResponse refresh(RefreshRequest request) {
        var result = refreshTokenService.validateAndRotate(request.refreshToken());

        var user = userQueryService.findById(result.userId())
                .orElseThrow(() -> new UserNotFoundException("User not found: " + result.userId()));

        if (user.getStatus() == UserStatus.SUSPENDED) {
            refreshTokenService.revokeAllForUser(user.getId());
            throw new DisabledException("Your account has been suspended. Contact K48 administration.");
        }

        var principal = new UserPrincipal(user);
        var accessToken = jwtTokenService.generateAccessToken(principal, user);

        return new RefreshResponse(
                accessToken,
                result.rawToken(),
                "Bearer",
                jwtConfig.getAccessTokenExpiry()
        );
    }

    void logout(LogoutRequest request) {
        // Extract user ID from refresh token for audit logging
        UUID userId = refreshTokenService.getUserIdFromToken(request.refreshToken());
        
        // Revoke the token
        refreshTokenService.revokeToken(request.refreshToken());
        
        // Log audit event if we could extract user ID
        if (userId != null) {
            auditService.log(userId, "LOGOUT", Map.of(
                    "userId", userId.toString()
            ));
        }
    }

    @org.springframework.transaction.annotation.Transactional
    void changePassword(UUID userId, ChangePasswordRequest request) {
        var user = userQueryService.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("User not found: " + userId));

        // Validate current password
        if (!passwordEncoder.matches(request.currentPassword(), user.getPasswordHash())) {
            throw new BadCredentialsException("Current password is incorrect.");
        }

        // Check if new password is the same as current password
        if (passwordEncoder.matches(request.newPassword(), user.getPasswordHash())) {
            throw new NewPasswordSameAsCurrentException("New password must be different from your current password.");
        }

        // Validate new password against password policy
        passwordPolicyService.validate(request.newPassword());

        // Update password
        user.setPasswordHash(passwordEncoder.encode(request.newPassword()));
        user.setRequiresPasswordChange(false);
        userQueryService.save(user);

        // Revoke all refresh tokens to force re-login with new password
        refreshTokenService.revokeAllForUser(userId);

        // Log audit event
        auditService.log(userId, "PASSWORD_CHANGED", Map.of(
                "userId", userId.toString(),
                "message", "User changed their password"
        ));
    }

}
