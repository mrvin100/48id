package io.k48.fortyeightid.operator.internal;

import io.k48.fortyeightid.audit.AuditService;
import io.k48.fortyeightid.auth.OperatorInviteTokenPort;
import io.k48.fortyeightid.identity.UserRoleService;
import jakarta.validation.Valid;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("${fortyeightid.api.prefix}/auth")
@RequiredArgsConstructor
class OperatorInviteController {

    private final OperatorInviteTokenPort operatorInviteTokenPort;
    private final OperatorAccountService operatorAccountService;
    private final UserRoleService userRoleService;
    private final AuditService auditService;

    @PostMapping("/accept-operator-invite")
    ResponseEntity<Map<String, Boolean>> acceptOperatorInvite(
            @Valid @RequestBody AcceptOperatorInviteRequest request) {

        UUID userId = operatorInviteTokenPort.validateAndConsumeInviteToken(request.token());

        operatorAccountService.acceptInvite(userId);

        userRoleService.changeRole(userId, "OPERATOR");

        auditService.log(userId, "OPERATOR_INVITE_ACCEPTED", Map.of("userId", userId.toString()));

        log.info("Operator invite accepted for user {}", userId);
        return ResponseEntity.ok(Map.of("success", true));
    }
}
