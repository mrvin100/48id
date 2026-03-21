package io.k48.fortyeightid.admin.internal;

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

    @GetMapping("/metrics")
    ResponseEntity<DashboardMetricsResponse> getDashboardMetrics() {
        var metrics = adminDashboardService.getDashboardMetrics();
        return ResponseEntity.ok(metrics);
    }

    @GetMapping("/login-activity")
    ResponseEntity<LoginActivityResponse> getLoginActivity() {
        var activity = adminDashboardService.getLoginActivity();
        return ResponseEntity.ok(activity);
    }

    @GetMapping("/recent-activity")
    ResponseEntity<RecentActivityResponse> getRecentActivity() {
        var activity = adminDashboardService.getRecentActivity();
        return ResponseEntity.ok(activity);
    }
}