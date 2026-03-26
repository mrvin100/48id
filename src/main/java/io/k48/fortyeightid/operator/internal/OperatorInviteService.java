package io.k48.fortyeightid.operator.internal;

import io.k48.fortyeightid.audit.AuditService;
import io.k48.fortyeightid.auth.OperatorInviteTokenPort;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
class OperatorInviteService {

    private final OperatorInviteTokenPort operatorInviteTokenPort;
    private final OperatorAccountService operatorAccountService;
    private final AuditService auditService;

    /**
     * Accepts an operator invite using only the token.
     * accountId is resolved from the token — the client does not need to supply it.
     */
    @Transactional
    void acceptInviteFlow(String token) {
        var payload = operatorInviteTokenPort.validateAndConsumeInviteToken(token);
        operatorAccountService.acceptInvite(payload.userId(), payload.accountId());
        auditService.log(payload.userId(), "OPERATOR_INVITE_ACCEPTED",
                Map.of("userId", payload.userId().toString(), "accountId", payload.accountId().toString()));
        log.info("Operator invite accepted for user {} on account {}", payload.userId(), payload.accountId());
    }
}
