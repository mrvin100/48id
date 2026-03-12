package io.k48.fortyeightid.auth;

/**
 * Public port for email operations.
 * Implemented by auth module internals.
 */
public interface EmailPort {

    /**
     * Sends an activation email with temporary password and activation link asynchronously.
     */
    void sendActivationEmail(String toEmail, String userName, String matricule, String temporaryPassword, String activationToken);

    /**
     * Sends a password reset email with reset token asynchronously.
     */
    void sendPasswordResetEmail(String toEmail, String userName, String resetToken);
}
