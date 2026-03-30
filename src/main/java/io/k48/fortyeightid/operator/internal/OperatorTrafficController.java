package io.k48.fortyeightid.operator.internal;

import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("${fortyeightid.api.prefix}/operator/traffic")
@PreAuthorize("hasRole('OPERATOR')")
@RequiredArgsConstructor
class OperatorTrafficController {

    private final OperatorTrafficService operatorTrafficService;
    private final OperatorAccountService operatorAccountService;

    @GetMapping
    ResponseEntity<OperatorTrafficResponse> getTraffic(
            @RequestParam UUID accountId,
            @AuthenticationPrincipal String callerId) {
        // Verify caller is an active member of this account
        operatorAccountService.requireActiveMember(accountId, UUID.fromString(callerId));
        return ResponseEntity.ok(operatorTrafficService.getTrafficForAccount(accountId));
    }
}
