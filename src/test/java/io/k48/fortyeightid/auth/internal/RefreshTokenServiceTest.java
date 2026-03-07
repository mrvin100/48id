package io.k48.fortyeightid.auth.internal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.k48.fortyeightid.shared.exception.RefreshTokenInvalidException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Duration;
import java.util.HexFormat;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.SetOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

@ExtendWith(MockitoExtension.class)
class RefreshTokenServiceTest {

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOps;

    @Mock
    private SetOperations<String, String> setOps;

    @InjectMocks
    private RefreshTokenService refreshTokenService;

    private void setupMocks() {
        when(redisTemplate.opsForValue()).thenReturn(valueOps);
        when(redisTemplate.opsForSet()).thenReturn(setOps);
    }

    private String sha256(String input) throws Exception {
        var digest = MessageDigest.getInstance("SHA-256");
        return HexFormat.of().formatHex(digest.digest(input.getBytes(StandardCharsets.UTF_8)));
    }

    @Test
    void createRefreshToken_storesInRedis() {
        setupMocks();
        var userId = UUID.randomUUID();

        var token = refreshTokenService.createRefreshToken(userId);

        assertThat(token).isNotBlank();
        verify(valueOps).set(anyString(), eq(userId.toString()), any(Duration.class));
        verify(setOps).add(eq("user_rt:" + userId), anyString());
    }

    @Test
    void validateAndRotate_returnsNewToken() throws Exception {
        setupMocks();
        var rawToken = UUID.randomUUID().toString();
        var hash = sha256(rawToken);
        var userId = UUID.randomUUID();

        when(valueOps.get("rt:" + hash)).thenReturn(userId.toString());
        when(redisTemplate.delete(anyString())).thenReturn(true);

        var result = refreshTokenService.validateAndRotate(rawToken);

        assertThat(result.userId()).isEqualTo(userId);
        assertThat(result.rawToken()).isNotBlank();
        assertThat(result.rawToken()).isNotEqualTo(rawToken);
        verify(redisTemplate).delete("rt:" + hash);
    }

    @Test
    void validateAndRotate_throwsOnInvalidToken() {
        when(redisTemplate.opsForValue()).thenReturn(valueOps);
        when(valueOps.get(anyString())).thenReturn(null);

        assertThatThrownBy(() -> refreshTokenService.validateAndRotate("bad-token"))
                .isInstanceOf(RefreshTokenInvalidException.class);
    }

    @Test
    void revokeToken_deletesFromRedis() {
        when(redisTemplate.opsForValue()).thenReturn(valueOps);
        when(redisTemplate.opsForSet()).thenReturn(setOps);
        var userId = UUID.randomUUID().toString();
        when(valueOps.get(anyString())).thenReturn(userId);
        when(redisTemplate.delete(anyString())).thenReturn(true);

        refreshTokenService.revokeToken("some-token");

        verify(redisTemplate).delete(anyString());
    }

    @Test
    void revokeAllForUser_deletesAllUserTokens() {
        when(redisTemplate.opsForSet()).thenReturn(setOps);
        var userId = UUID.randomUUID();
        when(setOps.members("user_rt:" + userId)).thenReturn(Set.of("hash1", "hash2"));

        refreshTokenService.revokeAllForUser(userId);

        verify(redisTemplate).delete(eq("user_rt:" + userId));
    }
}
