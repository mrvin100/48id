package io.k48.fortyeightid.auth;

import io.k48.fortyeightid.identity.User;

/**
 * Public port for password-reset and account-activation flows.
 * Implemented by the auth module internals.
 */
public interface PasswordResetPort {

    /**
     * Generates a password reset token, persists it, and dispatches
     * the reset email asynchronously. All existing password reset tokens
     * for the user are invalidated first.
     */
    void initiatePasswordReset(User user);

    /**
     * Generates an activation token, persists it, and dispatches
     * the activation email asynchronously. All existing activation tokens
     * for the user are invalidated first.
     */
    void initiateActivation(User user, String temporaryPassword);
}
