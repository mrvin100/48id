package io.k48.fortyeightid.admin.internal;

import io.k48.fortyeightid.admin.TrafficQueryPort;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("${fortyeightid.api.prefix}/admin/dashboard")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
class AdminDashboardController {

    private final AdminDashboardService adminDashboardService;
    private final TrafficQueryPort trafficQueryPort;

    @GetMapping("/metrics")
    ResponseEntity<DashboardMetricsResponse> getDashboardMetrics() {
        return ResponseEntity.ok(adminDashboardService.getDashboardMetrics());
    }

    @GetMapping("/login-activity")
    ResponseEntity<LoginActivityResponse> getLoginActivity() {
        return ResponseEntity.ok(adminDashboardService.getLoginActivity());
    }

    @GetMapping("/recent-activity")
    ResponseEntity<RecentActivityResponse> getRecentActivity() {
        return ResponseEntity.ok(adminDashboardService.getRecentActivity());
    }

    @GetMapping("/traffic")
    ResponseEntity<TrafficQueryPort.AggregatedTrafficView> getAggregatedTraffic() {
        return ResponseEntity.ok(trafficQueryPort.getAggregatedTraffic());
    }
}
