package io.k48.fortyeightid.auth;

import io.k48.fortyeightid.auth.internal.RefreshTokenService;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class TokenRevocationService {

    private final RefreshTokenService refreshTokenService;

    public void revokeAllTokensForUser(UUID userId) {
        refreshTokenService.revokeAllForUser(userId);
    }
}
