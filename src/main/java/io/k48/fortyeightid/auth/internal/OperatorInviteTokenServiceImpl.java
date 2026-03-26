package io.k48.fortyeightid.auth.internal;

import io.k48.fortyeightid.auth.OperatorInviteTokenPort;
import io.k48.fortyeightid.operator.internal.OperatorInviteTokenRepository;
import io.k48.fortyeightid.operator.internal.OperatorInviteToken;
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

    private final OperatorInviteTokenRepository repository;

    @Override
    @Transactional
    public String createInviteToken(UUID userId, UUID accountId, long ttlSeconds) {
        if (ttlSeconds <= 0) throw new IllegalArgumentException("ttlSeconds must be positive");
        // Invalidate any existing pending invite for this user+account
        repository.deleteAllByUserId(userId);
        var raw = UUID.randomUUID().toString();
        repository.save(OperatorInviteToken.builder()
                .token(raw)
                .userId(userId)
                .accountId(accountId)
                .expiresAt(Instant.now().plusSeconds(ttlSeconds))
                .build());
        return raw;
    }

    @Override
    @Transactional
    public InviteTokenPayload validateAndConsumeInviteToken(String raw) {
        var token = repository.findByToken(raw)
                .orElseThrow(() -> new ResetTokenInvalidException("Invalid invite token."));
        if (token.isUsed())
            throw new ResetTokenInvalidException("Invalid invite token.");
        if (token.getExpiresAt().isBefore(Instant.now()))
            throw new ResetTokenExpiredException("This invite link has expired. Please request a new one.");
        token.setUsed(true);
        repository.save(token);
        return new InviteTokenPayload(token.getUserId(), token.getAccountId());
    }
}
