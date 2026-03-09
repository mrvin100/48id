package io.k48.fortyeightid.admin.internal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.k48.fortyeightid.audit.AuditService;
import io.k48.fortyeightid.identity.Role;
import io.k48.fortyeightid.identity.User;
import io.k48.fortyeightid.identity.UserQueryService;
import io.k48.fortyeightid.identity.UserRoleService;
import io.k48.fortyeightid.identity.UserStatus;
import io.k48.fortyeightid.shared.exception.CannotChangeOwnRoleException;
import io.k48.fortyeightid.shared.exception.CannotPromoteSuspendedUserException;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AdminUserServiceTest {

    @Mock private UserQueryService userQueryService;
    @Mock private UserRoleService userRoleService;
    @Mock private AuditService auditService;

    @InjectMocks private AdminUserService adminUserService;

    private User activeUser(UUID id) {
        return User.builder()
                .id(id)
                .matricule("K48-2024-001")
                .email("test@k48.io")
                .name("Test User")
                .passwordHash("$2a$10$hash")
                .status(UserStatus.ACTIVE)
                .build();
    }

    @Test
    void changeRole_succeeds() {
        var targetId = UUID.randomUUID();
        var adminId = UUID.randomUUID();
        var user = activeUser(targetId);
        when(userQueryService.findById(targetId)).thenReturn(Optional.of(user));
        when(userRoleService.changeRole(targetId, "ADMIN")).thenReturn(user);

        var result = adminUserService.changeRole(targetId, "ADMIN", adminId);

        assertThat(result).isEqualTo(user);
        verify(auditService).log(eq(adminId), eq("ROLE_CHANGED"), any(Map.class));
    }

    @Test
    void changeRole_throwsOnSelfChange() {
        var id = UUID.randomUUID();

        assertThatThrownBy(() -> adminUserService.changeRole(id, "STUDENT", id))
                .isInstanceOf(CannotChangeOwnRoleException.class);

        verify(userRoleService, never()).changeRole(any(), anyString());
    }

    @Test
    void changeRole_throwsOnPromotingSuspendedUser() {
        var targetId = UUID.randomUUID();
        var adminId = UUID.randomUUID();
        var user = activeUser(targetId);
        user.setStatus(UserStatus.SUSPENDED);
        when(userQueryService.findById(targetId)).thenReturn(Optional.of(user));

        assertThatThrownBy(() -> adminUserService.changeRole(targetId, "ADMIN", adminId))
                .isInstanceOf(CannotPromoteSuspendedUserException.class);

        verify(userRoleService, never()).changeRole(any(), anyString());
    }
}
