package io.k48.fortyeightid.operator.internal;

import io.k48.fortyeightid.auth.OperatorInviteTokenPort;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * Delegates invite token creation to the auth module via port.
 */
@Service
@RequiredArgsConstructor
class OperatorInviteTokenService {

    private final OperatorInviteTokenPort operatorInviteTokenPort;

    String createInviteToken(UUID userId, long ttlSeconds) {
        return operatorInviteTokenPort.createInviteToken(userId, ttlSeconds);
    }
}
