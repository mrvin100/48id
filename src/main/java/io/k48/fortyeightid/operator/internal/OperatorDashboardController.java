package io.k48.fortyeightid.operator.internal;

import io.k48.fortyeightid.shared.DashboardQueryPort;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("${fortyeightid.api.prefix}/operator/dashboard")
@PreAuthorize("hasRole('OPERATOR')")
@RequiredArgsConstructor
class OperatorDashboardController {

    private final DashboardQueryPort dashboardQueryPort;

    @GetMapping("/metrics")
    ResponseEntity<OperatorDashboardResponse> getDashboardMetrics() {
        var snapshot = dashboardQueryPort.getDashboardSnapshot();
        var response = new OperatorDashboardResponse(
                snapshot.totalUsers(),
                snapshot.activeUsers(),
                snapshot.activeSessions(),
                snapshot.pendingActivations(),
                snapshot.suspendedUsers()
        );
        return ResponseEntity.ok(response);
    }
}
