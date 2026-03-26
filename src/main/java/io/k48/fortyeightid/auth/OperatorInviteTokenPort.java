package io.k48.fortyeightid.auth;

import java.util.UUID;

/**
 * Port for operator invite token operations.
 * Token is self-contained: it carries both userId and accountId.
 */
public interface OperatorInviteTokenPort {

    /**
     * Creates an invite token for a user + account pair.
     *
     * @return raw token to embed in the invite email URL
     */
    String createInviteToken(UUID userId, UUID accountId, long ttlSeconds);

    /**
     * Validates and consumes the token.
     *
     * @return the resolved userId + accountId
     */
    InviteTokenPayload validateAndConsumeInviteToken(String token);

    record InviteTokenPayload(UUID userId, UUID accountId) {}
}
