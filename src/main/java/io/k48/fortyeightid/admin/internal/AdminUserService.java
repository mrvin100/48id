package io.k48.fortyeightid.admin.internal;

import io.k48.fortyeightid.audit.AuditService;
import io.k48.fortyeightid.auth.LoginAttemptPort;
import io.k48.fortyeightid.auth.PasswordResetPort;
import io.k48.fortyeightid.auth.TokenRevocationService;
import io.k48.fortyeightid.identity.User;
import io.k48.fortyeightid.identity.UserQueryService;
import io.k48.fortyeightid.identity.UserRoleService;
import io.k48.fortyeightid.identity.UserStatus;
import io.k48.fortyeightid.identity.UserStatusService;
import io.k48.fortyeightid.identity.UserUpdateService;
import io.k48.fortyeightid.shared.exception.CannotChangeOwnRoleException;
import io.k48.fortyeightid.shared.exception.CannotDeleteOwnAccountException;
import io.k48.fortyeightid.shared.exception.CannotPromoteSuspendedUserException;
import io.k48.fortyeightid.shared.exception.MatriculeImmutableException;
import io.k48.fortyeightid.shared.exception.UserNotFoundException;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
class AdminUserService {

    private final UserQueryService userQueryService;
    private final UserRoleService userRoleService;
    private final UserStatusService userStatusService;
    private final UserUpdateService userUpdateService;
    private final AuditService auditService;
    private final TokenRevocationService tokenRevocationService;
    private final PasswordResetPort passwordResetService;
    private final LoginAttemptPort loginAttemptService;

    Page<User> listUsers(UserStatus status, String batch, String role, Pageable pageable) {
        return userQueryService.findAll(status, batch, role, pageable);
    }

    User getUser(UUID userId) {
        return userQueryService.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("User not found: " + userId));
    }

    User changeRole(UUID targetUserId, String newRole, UUID adminUserId) {
        if (targetUserId.equals(adminUserId)) {
            throw new CannotChangeOwnRoleException("Admins cannot change their own role");
        }
        if ("OPERATOR".equals(newRole)) {
            throw new IllegalArgumentException("OPERATOR role cannot be assigned by admin. It is earned through operator account ownership.");
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

    User updateUser(UUID targetUserId, UpdateUserRequest request, UUID adminUserId) {
        if (request.matricule() != null) {
            throw new MatriculeImmutableException("Matricule cannot be changed after account creation");
        }

        var result = userUpdateService.updateProfile(
                targetUserId, request.email(), request.name(), request.phone(),
                request.batch(), request.specialization());

        if (!result.changedFields().isEmpty()) {
            auditService.log(adminUserId, "ADMIN_USER_UPDATED", Map.of(
                    "changedBy", adminUserId.toString(),
                    "targetUser", targetUserId.toString(),
                    "changedFields", result.changedFields()
            ));
        }

        return result.user();
    }

    void forcePasswordReset(UUID targetUserId, UUID adminUserId) {
        var user = userQueryService.findById(targetUserId)
                .orElseThrow(() -> new UserNotFoundException("User not found: " + targetUserId));

        tokenRevocationService.revokeAllTokensForUser(targetUserId);
        passwordResetService.initiatePasswordReset(user);

        auditService.log(adminUserId, "ADMIN_FORCE_PASSWORD_RESET", Map.of(
                "performedBy", adminUserId.toString(),
                "targetUser", targetUserId.toString()
        ));
    }

    void softDeleteUser(UUID targetUserId, UUID adminUserId) {
        if (targetUserId.equals(adminUserId)) {
            throw new CannotDeleteOwnAccountException("Admins cannot delete their own account");
        }

        userStatusService.changeStatus(targetUserId, UserStatus.SUSPENDED);
        tokenRevocationService.revokeAllTokensForUser(targetUserId);

        auditService.log(adminUserId, "ACCOUNT_SUSPENDED", Map.of(
                "changedBy", adminUserId.toString(),
                "targetUser", targetUserId.toString(),
                "reason", "soft-delete"
        ));
    }

    void unlockAccount(UUID targetUserId, UUID adminUserId) {
        var user = userQueryService.findById(targetUserId)
                .orElseThrow(() -> new UserNotFoundException("User not found: " + targetUserId));

        loginAttemptService.unlockAccount(user.getMatricule(), adminUserId);

        auditService.log(adminUserId, "ACCOUNT_UNLOCKED_BY_ADMIN", Map.of(
                "performedBy", adminUserId.toString(),
                "targetUser", targetUserId.toString(),
                "matricule", user.getMatricule()
        ));
    }
}
