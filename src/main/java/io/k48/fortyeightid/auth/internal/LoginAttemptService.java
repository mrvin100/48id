package io.k48.fortyeightid.auth.internal;

import io.k48.fortyeightid.audit.AuditService;
import java.time.Duration;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class LoginAttemptService {

    private static final int MAX_ATTEMPTS = 5;
    private static final long LOCKOUT_TIME_SECONDS = 900; // 15 minutes
    private static final String LOCKOUT_PREFIX = "lockout:";
    private static final String ATTEMPTS_PREFIX = "attempts:";

    private final StringRedisTemplate redisTemplate;
    private final AuditService auditService;

    /**
     * Records a failed login attempt and locks the account if max attempts reached.
     * @param matricule The user's matricule
     * @return true if account is now locked, false otherwise
     */
    public boolean recordFailedAttempt(String matricule) {
        String attemptsKey = ATTEMPTS_PREFIX + matricule;
        String lockoutKey = LOCKOUT_PREFIX + matricule;

        // Check if already locked
        if (isLocked(matricule)) {
            return true;
        }

        // Increment attempt counter
        Long attempts = redisTemplate.opsForValue().increment(attemptsKey);
        redisTemplate.expire(attemptsKey, LOCKOUT_TIME_SECONDS, TimeUnit.SECONDS);

        if (attempts != null && attempts >= MAX_ATTEMPTS) {
            // Lock the account
            redisTemplate.opsForValue().set(lockoutKey, "locked", LOCKOUT_TIME_SECONDS, TimeUnit.SECONDS);
            redisTemplate.delete(attemptsKey);
            
            log.warn("Account locked due to too many failed login attempts: {}", matricule);
            return true;
        }

        return false;
    }

    /**
     * Resets the failed attempt counter after successful login.
     * @param matricule The user's matricule
     */
    public void resetFailedAttempts(String matricule) {
        String attemptsKey = ATTEMPTS_PREFIX + matricule;
        String lockoutKey = LOCKOUT_PREFIX + matricule;
        
        redisTemplate.delete(attemptsKey);
        redisTemplate.delete(lockoutKey);
    }

    /**
     * Checks if an account is currently locked.
     * @param matricule The user's matricule
     * @return true if locked, false otherwise
     */
    public boolean isLocked(String matricule) {
        String lockoutKey = LOCKOUT_PREFIX + matricule;
        return Boolean.TRUE.equals(redisTemplate.hasKey(lockoutKey));
    }

    /**
     * Gets the remaining lockout time in seconds.
     * @param matricule The user's matricule
     * @return remaining seconds, or 0 if not locked
     */
    public long getRemainingLockoutTime(String matricule) {
        String lockoutKey = LOCKOUT_PREFIX + matricule;
        Long ttl = redisTemplate.getExpire(lockoutKey);
        return ttl != null && ttl > 0 ? ttl : 0;
    }

    /**
     * Manually unlocks an account (admin action).
     * @param matricule The user's matricule
     * @param adminId The admin performing the unlock
     */
    public void unlockAccount(String matricule, UUID adminId) {
        String attemptsKey = ATTEMPTS_PREFIX + matricule;
        String lockoutKey = LOCKOUT_PREFIX + matricule;
        
        redisTemplate.delete(attemptsKey);
        redisTemplate.delete(lockoutKey);
        
        log.info("Account manually unlocked by admin: matricule={}, adminId={}", matricule, adminId);
    }

    /**
     * Gets the number of failed attempts for a user.
     * @param matricule The user's matricule
     * @return number of failed attempts, or 0 if none
     */
    public int getFailedAttempts(String matricule) {
        String attemptsKey = ATTEMPTS_PREFIX + matricule;
        String value = redisTemplate.opsForValue().get(attemptsKey);
        return value != null ? Integer.parseInt(value) : 0;
    }
}
