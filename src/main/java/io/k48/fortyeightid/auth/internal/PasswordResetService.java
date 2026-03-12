package io.k48.fortyeightid.auth.internal;

import io.k48.fortyeightid.audit.AuditService;
import io.k48.fortyeightid.auth.EmailPort;
import io.k48.fortyeightid.auth.PasswordPolicyService;
import io.k48.fortyeightid.auth.PasswordResetPort;
import io.k48.fortyeightid.identity.User;
import io.k48.fortyeightid.identity.UserQueryService;
import io.k48.fortyeightid.identity.UserStatus;
import io.k48.fortyeightid.shared.exception.ResetTokenExpiredException;
import io.k48.fortyeightid.shared.exception.ResetTokenInvalidException;
import io.k48.fortyeightid.shared.exception.UserNotFoundException;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
class PasswordResetService implements PasswordResetPort {

    private static final long RESET_TOKEN_TTL_SECONDS = 3600;
    private static final long ACTIVATION_TOKEN_TTL_SECONDS = 86400;

    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final EmailPort emailService;
    private final UserQueryService userQueryService;
    private final AuditService auditService;
    private final PasswordEncoder passwordEncoder;
    private final RefreshTokenService refreshTokenService;
    private final PasswordPolicyService passwordPolicyService;

    @Override
    @Transactional
    public void initiatePasswordReset(User user) {
        passwordResetTokenRepository.deleteAllByUserIdAndPurpose(user.getId(), ResetTokenPurpose.PASSWORD_RESET);
        var rawToken = createToken(user.getId(), ResetTokenPurpose.PASSWORD_RESET, RESET_TOKEN_TTL_SECONDS);
        emailService.sendPasswordResetEmail(user.getEmail(), user.getName(), rawToken);
        log.info("Password reset initiated for user {}", user.getId());
    }

    @Override
    @Transactional
    public void initiateActivation(User user, String temporaryPassword) {
        passwordResetTokenRepository.deleteAllByUserIdAndPurpose(user.getId(), ResetTokenPurpose.ACCOUNT_ACTIVATION);
        var rawToken = createToken(user.getId(), ResetTokenPurpose.ACCOUNT_ACTIVATION, ACTIVATION_TOKEN_TTL_SECONDS);
        emailService.sendActivationEmail(user.getEmail(), user.getName(), user.getMatricule(), temporaryPassword, rawToken);
        log.info("Activation initiated for user {}", user.getId());
    }

    @Transactional
    public void handleForgotPassword(String email) {
        var userOpt = userQueryService.findByEmail(email);

        if (userOpt.isPresent()) {
            var user = userOpt.get();
            initiatePasswordReset(user);
            auditService.log(user.getId(), "PASSWORD_RESET_REQUESTED", Map.of(
                    "email", email,
                    "userId", user.getId().toString()
            ));
        } else {
            log.info("Password reset requested for non-existent email: {}", email);
        }
    }

    @Transactional
    public void resetPassword(String token, String newPassword) {
        var resetToken = validateToken(token, ResetTokenPurpose.PASSWORD_RESET);
        passwordPolicyService.validate(newPassword);

        var user = userQueryService.findById(resetToken.getUserId())
                .orElseThrow(() -> new UserNotFoundException("User not found: " + resetToken.getUserId()));

        user.setPasswordHash(passwordEncoder.encode(newPassword));
        user.setRequiresPasswordChange(false);
        userQueryService.save(user);

        resetToken.setUsed(true);
        refreshTokenService.revokeAllForUser(user.getId());

        auditService.log(user.getId(), "PASSWORD_RESET_COMPLETED", Map.of(
                "userId", user.getId().toString(),
                "message", "User reset their password via email link"
        ));

        log.info("Password reset completed for user {}", user.getId());
    }

    @Transactional
    public void activateAccount(String token) {
        var activationToken = validateToken(token, ResetTokenPurpose.ACCOUNT_ACTIVATION);
        var user = userQueryService.findById(activationToken.getUserId())
                .orElseThrow(() -> new UserNotFoundException("User not found: " + activationToken.getUserId()));

        if (user.getStatus() == UserStatus.SUSPENDED) {
            throw new DisabledException("Your account has been suspended. Contact K48 administration.");
        }

        user.setStatus(UserStatus.ACTIVE);
        userQueryService.save(user);
        activationToken.setUsed(true);

        auditService.log(user.getId(), "ACCOUNT_ACTIVATED", Map.of(
                "userId", user.getId().toString(),
                "matricule", user.getMatricule()
        ));

        log.info("Account activated for user {}", user.getId());
    }

    private String createToken(UUID userId, ResetTokenPurpose purpose, long ttlSeconds) {
        var rawToken = UUID.randomUUID().toString();
        var expiresAt = Instant.now().plusSeconds(ttlSeconds);

        var token = PasswordResetToken.builder()
                .userId(userId)
                .token(rawToken)
                .purpose(purpose)
                .expiresAt(expiresAt)
                .build();

        passwordResetTokenRepository.save(token);
        return rawToken;
    }

    private PasswordResetToken validateToken(String token, ResetTokenPurpose expectedPurpose) {
        var resetToken = passwordResetTokenRepository.findByToken(token)
                .orElseThrow(() -> new ResetTokenInvalidException("Invalid reset token."));

        if (resetToken.getPurpose() != expectedPurpose) {
            throw new ResetTokenInvalidException("Invalid reset token.");
        }
        if (resetToken.getExpiresAt().isBefore(Instant.now())) {
            throw new ResetTokenExpiredException("This reset link has expired. Please request a new one.");
        }
        if (resetToken.isUsed()) {
            throw new ResetTokenInvalidException("Invalid reset token.");
        }
        return resetToken;
    }
}
