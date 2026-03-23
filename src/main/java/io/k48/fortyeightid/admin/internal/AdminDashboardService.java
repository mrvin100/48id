package io.k48.fortyeightid.admin.internal;

import io.k48.fortyeightid.admin.DashboardQueryPort;
import io.k48.fortyeightid.audit.AuditLogRepository;
import io.k48.fortyeightid.identity.User;
import io.k48.fortyeightid.identity.UserQueryService;
import io.k48.fortyeightid.identity.UserStatus;
import io.k48.fortyeightid.shared.exception.UserNotFoundException;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
class AdminDashboardService implements DashboardQueryPort {

    private final UserQueryService userQueryService;
    private final AuditLogRepository auditLogRepository;

    DashboardMetricsResponse getDashboardMetrics() {
        // Get user counts by status
        long totalUsers = userQueryService.count();
        long activeUsers = userQueryService.countByStatus(UserStatus.ACTIVE);
        long pendingUsers = userQueryService.countByStatus(UserStatus.PENDING_ACTIVATION);
        long suspendedUsers = userQueryService.countByStatus(UserStatus.SUSPENDED);

        // Get active sessions count (users who logged in within last 24 hours)
        Instant last24Hours = Instant.now().minusSeconds(24 * 60 * 60);
        long activeSessions = auditLogRepository.countActiveSessionsSince(last24Hours);

        return new DashboardMetricsResponse(
                totalUsers,
                activeUsers,
                activeSessions,
                pendingUsers,
                suspendedUsers,
                "operational" // System health - could be enhanced with actual health checks
        );
    }

    LoginActivityResponse getLoginActivity() {
        // Get login activity for the last 7 days
        List<LoginActivityData> loginActivity = List.of();
        
        try {
            // Get data for last 7 days
            Instant sevenDaysAgo = Instant.now().minusSeconds(7 * 24 * 60 * 60);
            var loginEvents = auditLogRepository.findLoginActivitySince(sevenDaysAgo);
            
            // Group by day and count
            Map<String, Long> dailyCounts = loginEvents.stream()
                    .collect(Collectors.groupingBy(
                            audit -> LocalDate.ofInstant(audit.getCreatedAt(), ZoneOffset.UTC)
                                    .format(DateTimeFormatter.ofPattern("EEE")),
                            Collectors.counting()
                    ));

            // Create response for last 7 days
            loginActivity = List.of(
                    new LoginActivityData("Mon", dailyCounts.getOrDefault("Mon", 0L)),
                    new LoginActivityData("Tue", dailyCounts.getOrDefault("Tue", 0L)),
                    new LoginActivityData("Wed", dailyCounts.getOrDefault("Wed", 0L)),
                    new LoginActivityData("Thu", dailyCounts.getOrDefault("Thu", 0L)),
                    new LoginActivityData("Fri", dailyCounts.getOrDefault("Fri", 0L)),
                    new LoginActivityData("Sat", dailyCounts.getOrDefault("Sat", 0L)),
                    new LoginActivityData("Sun", dailyCounts.getOrDefault("Sun", 0L))
            );
        } catch (Exception e) {
            // Fallback to empty data if repository methods don't exist yet
            loginActivity = List.of(
                    new LoginActivityData("Mon", 0L),
                    new LoginActivityData("Tue", 0L),
                    new LoginActivityData("Wed", 0L),
                    new LoginActivityData("Thu", 0L),
                    new LoginActivityData("Fri", 0L),
                    new LoginActivityData("Sat", 0L),
                    new LoginActivityData("Sun", 0L)
            );
        }

        return new LoginActivityResponse(loginActivity);
    }

    RecentActivityResponse getRecentActivity() {
        try {
            // Get recent audit events (last 10)
            var recentEvents = auditLogRepository.findRecentActivity(PageRequest.of(0, 10));
            
            var activities = recentEvents.stream()
                    .map(audit -> new RecentActivityData(
                            audit.getAction(),
                            audit.getUserId() != null ? audit.getUserId().toString() : "System",
                            audit.getIpAddress(),
                            audit.getCreatedAt()
                    ))
                    .collect(Collectors.toList());

            return new RecentActivityResponse(activities);
        } catch (Exception e) {
            // Fallback to empty data if repository methods don't exist yet
            return new RecentActivityResponse(List.of());
        }
    }

    // --- DashboardQueryPort implementation ---

    @Override
    public DashboardSnapshot getDashboardSnapshot() {
        long totalUsers = userQueryService.count();
        long activeUsers = userQueryService.countByStatus(UserStatus.ACTIVE);
        long pendingUsers = userQueryService.countByStatus(UserStatus.PENDING_ACTIVATION);
        long suspendedUsers = userQueryService.countByStatus(UserStatus.SUSPENDED);
        long activeSessions = auditLogRepository.countActiveSessionsSince(
                Instant.now().minusSeconds(24 * 60 * 60));
        return new DashboardSnapshot(totalUsers, activeUsers, activeSessions, pendingUsers, suspendedUsers);
    }

    @Override
    public Page<User> listUsers(UserStatus status, String batch, String role, Pageable pageable) {
        return userQueryService.findAll(status, batch, role, pageable);
    }

    @Override
    public User getUser(UUID userId) {
        return userQueryService.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("User not found: " + userId));
    }
}