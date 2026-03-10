package io.k48.fortyeightid.auth;

import io.k48.fortyeightid.identity.User;

/**
 * Public port for initiating a password reset flow.
 * Implemented by the auth module internals.
 */
public interface PasswordResetPort {

    /**
     * Generates a password reset token, persists it, and dispatches
     * the reset email asynchronously. All existing tokens for the user
     * are invalidated first.
     */
    void initiatePasswordReset(User user);
}
