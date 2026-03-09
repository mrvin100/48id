package io.k48.fortyeightid.auth.internal;

import io.k48.fortyeightid.shared.exception.RefreshTokenInvalidException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.util.HexFormat;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class RefreshTokenService {

    private static final String RT_PREFIX = "rt:";
    private static final String USER_RT_PREFIX = "user_rt:";
    private static final Duration REFRESH_TOKEN_TTL = Duration.ofDays(7);

    private final StringRedisTemplate redisTemplate;

    String createRefreshToken(UUID userId) {
        var rawToken = UUID.randomUUID().toString();
        var hash = sha256(rawToken);

        redisTemplate.opsForValue().set(RT_PREFIX + hash, userId.toString(), REFRESH_TOKEN_TTL);
        redisTemplate.opsForSet().add(USER_RT_PREFIX + userId, hash);
        redisTemplate.expire(USER_RT_PREFIX + userId, REFRESH_TOKEN_TTL);

        return rawToken;
    }

    RefreshTokenResult validateAndRotate(String rawToken) {
        var hash = sha256(rawToken);
        var userId = redisTemplate.opsForValue().get(RT_PREFIX + hash);

        if (userId == null) {
            throw new RefreshTokenInvalidException("Refresh token is invalid or expired");
        }

        // Revoke old token
        redisTemplate.delete(RT_PREFIX + hash);
        redisTemplate.opsForSet().remove(USER_RT_PREFIX + userId, hash);

        // Issue new token
        var newRawToken = UUID.randomUUID().toString();
        var newHash = sha256(newRawToken);
        redisTemplate.opsForValue().set(RT_PREFIX + newHash, userId, REFRESH_TOKEN_TTL);
        redisTemplate.opsForSet().add(USER_RT_PREFIX + userId, newHash);
        redisTemplate.expire(USER_RT_PREFIX + userId, REFRESH_TOKEN_TTL);

        return new RefreshTokenResult(UUID.fromString(userId), newRawToken);
    }

    void revokeToken(String rawToken) {
        var hash = sha256(rawToken);
        var userId = redisTemplate.opsForValue().get(RT_PREFIX + hash);
        redisTemplate.delete(RT_PREFIX + hash);
        if (userId != null) {
            redisTemplate.opsForSet().remove(USER_RT_PREFIX + userId, hash);
        }
    }

    public void revokeAllForUser(UUID userId) {
        Set<String> hashes = redisTemplate.opsForSet().members(USER_RT_PREFIX + userId);
        if (hashes != null && !hashes.isEmpty()) {
            var keys = hashes.stream().map(h -> RT_PREFIX + h).toList();
            redisTemplate.delete(keys);
        }
        redisTemplate.delete(USER_RT_PREFIX + userId);
    }

    private String sha256(String input) {
        try {
            var digest = MessageDigest.getInstance("SHA-256");
            var hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }

    record RefreshTokenResult(UUID userId, String rawToken) {}
}
