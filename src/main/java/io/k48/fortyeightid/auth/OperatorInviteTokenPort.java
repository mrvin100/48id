package io.k48.fortyeightid.auth;

import java.util.UUID;

/**
 * Public port for operator invite token operations.
 * Implemented by auth module internals.
 */
public interface OperatorInviteTokenPort {

    /**
     * Creates a new operator invite token for the given user.
     *
     * @param userId     the invited user's ID
     * @param ttlSeconds token time-to-live in seconds; must be positive
     * @return the raw (unhashed) token to include in the invite email
     * @throws IllegalArgumentException if ttlSeconds is not positive
     */
    String createInviteToken(UUID userId, long ttlSeconds);

    /**
     * Validates the invite token and returns the associated user ID.
     * Marks the token as used.
     *
     * @param token the raw token from the invite email
     * @return the user ID associated with the token
     * @throws io.k48.fortyeightid.shared.exception.ResetTokenInvalidException if token is invalid or already used
     * @throws io.k48.fortyeightid.shared.exception.ResetTokenExpiredException if token has expired
     */
    UUID validateAndConsumeInviteToken(String token);
}
