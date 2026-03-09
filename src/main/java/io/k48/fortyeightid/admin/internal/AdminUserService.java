package io.k48.fortyeightid.admin.internal;

import io.k48.fortyeightid.audit.AuditService;
import io.k48.fortyeightid.auth.TokenRevocationService;
import io.k48.fortyeightid.identity.User;
import io.k48.fortyeightid.identity.UserQueryService;
import io.k48.fortyeightid.identity.UserRoleService;
import io.k48.fortyeightid.identity.UserStatus;
import io.k48.fortyeightid.identity.UserStatusService;
import io.k48.fortyeightid.shared.exception.CannotChangeOwnRoleException;
import io.k48.fortyeightid.shared.exception.CannotPromoteSuspendedUserException;
import io.k48.fortyeightid.shared.exception.UserNotFoundException;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
class AdminUserService {

    private final UserQueryService userQueryService;
    private final UserRoleService userRoleService;
    private final UserStatusService userStatusService;
    private final AuditService auditService;
    private final TokenRevocationService tokenRevocationService;

    User getUser(UUID userId) {
        return userQueryService.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("User not found: " + userId));
    }

    User changeRole(UUID targetUserId, String newRole, UUID adminUserId) {
        if (targetUserId.equals(adminUserId)) {
            throw new CannotChangeOwnRoleException("Admins cannot change their own role");
        }

        var user = userQueryService.findById(targetUserId)
                .orElseThrow(() -> new UserNotFoundException("User not found: " + targetUserId));

        if (user.getStatus() == UserStatus.SUSPENDED && "ADMIN".equals(newRole)) {
            throw new CannotPromoteSuspendedUserException("Cannot promote a suspended user to admin");
        }

        var oldRoles = user.getRoles().stream()
                .map(r -> r.getName())
                .collect(Collectors.joining(","));

        var updated = userRoleService.changeRole(targetUserId, newRole);

        auditService.log(adminUserId, "ROLE_CHANGED", Map.of(
                "changedBy", adminUserId.toString(),
                "targetUser", targetUserId.toString(),
                "oldRole", oldRoles,
                "newRole", newRole
        ));

        return updated;
    }

    User changeStatus(UUID targetUserId, UserStatus newStatus, UUID adminUserId) {
        var user = userQueryService.findById(targetUserId)
                .orElseThrow(() -> new UserNotFoundException("User not found: " + targetUserId));

        var oldStatus = user.getStatus();
        var updated = userStatusService.changeStatus(targetUserId, newStatus);

        if (newStatus == UserStatus.SUSPENDED) {
            tokenRevocationService.revokeAllTokensForUser(targetUserId);
            auditService.log(adminUserId, "ACCOUNT_SUSPENDED", Map.of(
                    "changedBy", adminUserId.toString(),
                    "targetUser", targetUserId.toString(),
                    "oldStatus", oldStatus.name(),
                    "newStatus", newStatus.name()
            ));
        } else if (newStatus == UserStatus.ACTIVE) {
            auditService.log(adminUserId, "ACCOUNT_REACTIVATED", Map.of(
                    "changedBy", adminUserId.toString(),
                    "targetUser", targetUserId.toString(),
                    "oldStatus", oldStatus.name(),
                    "newStatus", newStatus.name()
            ));
        }

        return updated;
    }
}
