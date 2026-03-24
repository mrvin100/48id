package io.k48.fortyeightid.operator.internal;

import io.k48.fortyeightid.audit.AuditService;
import io.k48.fortyeightid.auth.OperatorInviteTokenPort;
import io.k48.fortyeightid.identity.UserRoleService;
import java.util.Map;
import java.util.UUID;
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
    private final UserRoleService userRoleService;
    private final AuditService auditService;

    @Transactional
    void acceptInviteFlow(String token, UUID accountId) {
        UUID userId = operatorInviteTokenPort.validateAndConsumeInviteToken(token);
        operatorAccountService.acceptInvite(userId, accountId);
        userRoleService.changeRole(userId, "OPERATOR");
        auditService.log(userId, "OPERATOR_INVITE_ACCEPTED", Map.of("userId", userId.toString()));
        log.info("Operator invite accepted for user {}", userId);
    }
}
