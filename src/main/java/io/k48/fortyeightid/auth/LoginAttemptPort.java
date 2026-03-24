package io.k48.fortyeightid.auth;

import java.util.UUID;

/**
 * Public port for login attempt tracking and account lockout.
 * Implemented by auth module internals.
 */
public interface LoginAttemptPort {

    /**
     * Records a failed login attempt and locks the account if max attempts reached.
     * @param matricule The user's matricule
     * @return true if account is now locked, false otherwise
     */
    boolean recordFailedAttempt(String matricule);

    /**
     * Resets the failed attempt counter after successful login.
     * @param matricule The user's matricule
     */
    void resetFailedAttempts(String matricule);

    /**
     * Checks if an account is currently locked.
     * @param matricule The user's matricule
     * @return true if locked, false otherwise
     */
    boolean isLocked(String matricule);

    /**
     * Gets the remaining lockout time in seconds.
     * @param matricule The user's matricule
     * @return remaining seconds, or 0 if not locked
     */
    long getRemainingLockoutTime(String matricule);

    /**
     * Manually unlocks an account (admin action).
     * @param matricule The user's matricule
     * @param adminId The admin performing the unlock
     */
    void unlockAccount(String matricule, UUID adminId);
}
