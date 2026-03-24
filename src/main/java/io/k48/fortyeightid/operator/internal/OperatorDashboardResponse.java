package io.k48.fortyeightid.operator.internal;

/**
 * Read-only dashboard snapshot for OPERATOR consumers.
 * Mirrors DashboardMetricsResponse without the mutable systemHealth field.
 */
record OperatorDashboardResponse(
        long totalUsers,
        long activeUsers,
        long activeSessions,
        long pendingActivations,
        long suspendedUsers
) {}
