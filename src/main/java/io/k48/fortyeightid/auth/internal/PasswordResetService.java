package io.k48.fortyeightid.auth.internal;

import io.k48.fortyeightid.auth.EmailPort;
import io.k48.fortyeightid.auth.PasswordResetPort;
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
class PasswordResetService implements PasswordResetPort {

    private static final long RESET_TOKEN_TTL_SECONDS = 3600; // 1 hour

    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final EmailPort emailService;

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
}
