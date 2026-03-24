package io.k48.fortyeightid.admin.internal;

record DashboardMetricsResponse(
        long totalUsers,
        long activeUsers,
        long activeSessions,
        long pendingActivations,
        long suspendedUsers,
        String systemHealth
) {
}