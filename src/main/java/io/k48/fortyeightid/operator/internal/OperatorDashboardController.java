package io.k48.fortyeightid.operator.internal;

import io.k48.fortyeightid.admin.DashboardQueryPort;
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
@RequestMapping("${fortyeightid.api.prefix}/operator/dashboard")
@PreAuthorize("hasRole('OPERATOR')")
@RequiredArgsConstructor
class OperatorDashboardController {

    private final DashboardQueryPort dashboardQueryPort;
    private final OperatorAccountService operatorAccountService;

    @GetMapping("/metrics")
    ResponseEntity<OperatorDashboardResponse> getDashboardMetrics(
            @RequestParam UUID accountId,
            @AuthenticationPrincipal String callerId) {

        operatorAccountService.requireActiveMember(accountId, UUID.fromString(callerId));

        var snapshot = dashboardQueryPort.getDashboardSnapshot();
        return ResponseEntity.ok(new OperatorDashboardResponse(
                snapshot.totalUsers(),
                snapshot.activeUsers(),
                snapshot.activeSessions(),
                snapshot.pendingActivations(),
                snapshot.suspendedUsers()
        ));
    }
}
