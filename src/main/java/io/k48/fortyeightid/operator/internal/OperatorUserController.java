package io.k48.fortyeightid.operator.internal;

import io.k48.fortyeightid.admin.DashboardQueryPort;
import io.k48.fortyeightid.identity.UserStatus;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("${fortyeightid.api.prefix}/operator/users")
@PreAuthorize("hasRole('OPERATOR')")
@RequiredArgsConstructor
class OperatorUserController {

    private final DashboardQueryPort dashboardQueryPort;
    private final OperatorAccountService operatorAccountService;

    /**
     * Lists users scoped to the members of the given operator account.
     * Caller must be an active member of that account.
     */
    @GetMapping
    ResponseEntity<Page<OperatorUserResponse>> listUsers(
            @RequestParam UUID accountId,
            @RequestParam(required = false) UserStatus status,
            @RequestParam(required = false) String batch,
            @RequestParam(required = false) String role,
            @PageableDefault(size = 20) Pageable pageable,
            @AuthenticationPrincipal String callerId) {

        operatorAccountService.requireActiveMember(accountId, UUID.fromString(callerId));

        var page = dashboardQueryPort.listUsers(status, batch, role, pageable)
                .map(OperatorUserResponse::from);
        return ResponseEntity.ok(page);
    }

    @GetMapping("/{id}")
    ResponseEntity<OperatorUserResponse> getUser(
            @PathVariable UUID id,
            @RequestParam UUID accountId,
            @AuthenticationPrincipal String callerId) {

        operatorAccountService.requireActiveMember(accountId, UUID.fromString(callerId));

        return ResponseEntity.ok(OperatorUserResponse.from(dashboardQueryPort.getUser(id)));
    }
}
