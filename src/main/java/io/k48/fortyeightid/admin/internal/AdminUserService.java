package io.k48.fortyeightid.admin.internal;

import io.k48.fortyeightid.audit.AuditService;
import io.k48.fortyeightid.identity.User;
import io.k48.fortyeightid.identity.UserQueryService;
import io.k48.fortyeightid.identity.UserRoleService;
import io.k48.fortyeightid.identity.UserStatus;
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
    private final AuditService auditService;

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
}
