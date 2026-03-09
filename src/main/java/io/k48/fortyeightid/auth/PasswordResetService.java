package io.k48.fortyeightid.auth;

import io.k48.fortyeightid.auth.internal.EmailService;
import io.k48.fortyeightid.auth.internal.PasswordResetToken;
import io.k48.fortyeightid.auth.internal.PasswordResetTokenRepository;
import io.k48.fortyeightid.identity.User;
import java.time.Instant;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class PasswordResetService {

    private static final long RESET_TOKEN_TTL_SECONDS = 3600; // 1 hour

    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final EmailService emailService;

    /**
     * Generates a password reset token for the given user, persists it,
     * and dispatches the reset email asynchronously.
     * All existing tokens for the user are invalidated first.
     */
    @Transactional
    public void initiatePasswordReset(User user) {
        // Invalidate any existing tokens for this user
        passwordResetTokenRepository.deleteAllByUserId(user.getId());

        var rawToken = UUID.randomUUID().toString();
        var expiresAt = Instant.now().plusSeconds(RESET_TOKEN_TTL_SECONDS);

        var resetToken = PasswordResetToken.builder()
                .userId(user.getId())
                .token(rawToken)
                .expiresAt(expiresAt)
                .build();

        passwordResetTokenRepository.save(resetToken);

        // Send email asynchronously — failure is logged but does not fail this call
        emailService.sendPasswordResetEmail(user.getEmail(), user.getName(), rawToken);

        log.info("Password reset initiated for user {}", user.getId());
    }
}
