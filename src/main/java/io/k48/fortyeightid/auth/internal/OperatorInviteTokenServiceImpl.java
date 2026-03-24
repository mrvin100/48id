package io.k48.fortyeightid.auth.internal;

import io.k48.fortyeightid.auth.OperatorInviteTokenPort;
import io.k48.fortyeightid.shared.exception.ResetTokenExpiredException;
import io.k48.fortyeightid.shared.exception.ResetTokenInvalidException;
import java.time.Instant;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
class OperatorInviteTokenServiceImpl implements OperatorInviteTokenPort {

    private final PasswordResetTokenRepository passwordResetTokenRepository;

    @Override
    @Transactional
    public String createInviteToken(UUID userId, long ttlSeconds) {
        if (ttlSeconds <= 0) {
            throw new IllegalArgumentException("ttlSeconds must be positive, got: " + ttlSeconds);
        }
        passwordResetTokenRepository.deleteAllByUserIdAndPurpose(userId, ResetTokenPurpose.OPERATOR_INVITE);
        var rawToken = UUID.randomUUID().toString();
        var token = PasswordResetToken.builder()
                .userId(userId)
                .token(rawToken)
                .purpose(ResetTokenPurpose.OPERATOR_INVITE)
                .expiresAt(Instant.now().plusSeconds(ttlSeconds))
                .build();
        passwordResetTokenRepository.save(token);
        return rawToken;
    }

    @Override
    @Transactional
    public UUID validateAndConsumeInviteToken(String rawToken) {
        var token = passwordResetTokenRepository.findByToken(rawToken)
                .orElseThrow(() -> new ResetTokenInvalidException("Invalid invite token."));

        if (token.getPurpose() != ResetTokenPurpose.OPERATOR_INVITE) {
            throw new ResetTokenInvalidException("Invalid invite token.");
        }
        if (token.getExpiresAt().isBefore(Instant.now())) {
            throw new ResetTokenExpiredException("This invite link has expired. Please request a new one.");
        }
        if (token.isUsed()) {
            throw new ResetTokenInvalidException("Invalid invite token.");
        }

        token.setUsed(true);
        passwordResetTokenRepository.save(token);
        return token.getUserId();
    }
}
