package io.k48.fortyeightid.admin;

import io.k48.fortyeightid.identity.UserStatus;
import io.k48.fortyeightid.identity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

/**
 * Public port exposing read-only dashboard and user data for cross-module consumers (e.g. operator module).
 * Implementations live in admin/internal and must not be accessed directly.
 */
public interface DashboardQueryPort {

    DashboardSnapshot getDashboardSnapshot();

    Page<User> listUsers(UserStatus status, String batch, String role, Pageable pageable);

    User getUser(java.util.UUID userId);

    /**
     * Immutable snapshot of dashboard metrics — safe to cross module boundaries.
     */
    record DashboardSnapshot(
            long totalUsers,
            long activeUsers,
            long activeSessions,
            long pendingActivations,
            long suspendedUsers
    ) {}
}
