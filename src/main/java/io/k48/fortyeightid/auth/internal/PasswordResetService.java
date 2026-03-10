package io.k48.fortyeightid.auth.internal;

import io.k48.fortyeightid.audit.AuditService;
import io.k48.fortyeightid.auth.EmailPort;
import io.k48.fortyeightid.auth.PasswordResetPort;
import io.k48.fortyeightid.identity.User;
import io.k48.fortyeightid.identity.UserQueryService;
import io.k48.fortyeightid.shared.exception.PasswordPolicyViolationException;
import io.k48.fortyeightid.shared.exception.ResetTokenExpiredException;
import io.k48.fortyeightid.shared.exception.ResetTokenInvalidException;
import io.k48.fortyeightid.shared.exception.UserNotFoundException;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Pattern;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
class PasswordResetService implements PasswordResetPort {

    private static final long RESET_TOKEN_TTL_SECONDS = 3600; // 1 hour
    private static final Pattern PASSWORD_PATTERN = Pattern.compile(
            "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&])[A-Za-z\\d@$!%*?&]{8,}$"
    );

    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final EmailPort emailService;
    private final UserQueryService userQueryService;
    private final AuditService auditService;
    private final PasswordEncoder passwordEncoder;
    private final RefreshTokenService refreshTokenService;

    @Override
    @Transactional
    public void initiatePasswordReset(User user) {
        passwordResetTokenRepository.deleteAllByUserId(user.getId());

        var rawToken = UUID.randomUUID().toString();
        var expiresAt = Instant.now().plusSeconds(RESET_TOKEN_TTL_SECONDS);

        var resetToken = PasswordResetToken.builder()
                .userId(user.getId())
                .token(rawToken)
                .expiresAt(expiresAt)
                .build();

        passwordResetTokenRepository.save(resetToken);

        emailService.sendPasswordResetEmail(user.getEmail(), user.getName(), rawToken);

        log.info("Password reset initiated for user {}", user.getId());
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
        // Find and validate token
        var resetTokenOpt = passwordResetTokenRepository.findByToken(token);
        
        if (resetTokenOpt.isEmpty()) {
            throw new ResetTokenInvalidException("Invalid reset token.");
        }

        var resetToken = resetTokenOpt.get();

        // Check if token is expired
        if (resetToken.getExpiresAt().isBefore(Instant.now())) {
            throw new ResetTokenExpiredException("This reset link has expired. Please request a new one.");
        }

        // Check if token is already used
        if (resetToken.isUsed()) {
            throw new ResetTokenInvalidException("Invalid reset token.");
        }

        // Validate password policy
        validatePasswordPolicy(newPassword);

        // Find user and update password
        var user = userQueryService.findById(resetToken.getUserId())
                .orElseThrow(() -> new UserNotFoundException("User not found: " + resetToken.getUserId()));

        user.setPasswordHash(passwordEncoder.encode(newPassword));
        user.setRequiresPasswordChange(false);
        userQueryService.save(user);

        // Mark token as used
        resetToken.setUsed(true);

        // Revoke all refresh tokens for this user
        refreshTokenService.revokeAllForUser(user.getId());

        // Log audit event
        auditService.log(user.getId(), "PASSWORD_RESET_COMPLETED", Map.of(
                "userId", user.getId().toString(),
                "message", "User reset their password via email link"
        ));

        log.info("Password reset completed for user {}", user.getId());
    }

    private void validatePasswordPolicy(String password) {
        if (!PASSWORD_PATTERN.matcher(password).matches()) {
            throw new PasswordPolicyViolationException(
                    "Password must be at least 8 characters long and contain at least one uppercase letter, " +
                    "one lowercase letter, one digit, and one special character (@$!%*?&)."
            );
        }
    }
}
