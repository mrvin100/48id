package io.k48.fortyeightid.admin.internal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.k48.fortyeightid.audit.AuditService;
import io.k48.fortyeightid.auth.LoginAttemptPort;
import io.k48.fortyeightid.identity.Role;
import io.k48.fortyeightid.identity.User;
import io.k48.fortyeightid.identity.UserQueryService;
import io.k48.fortyeightid.identity.UserStatus;
import io.k48.fortyeightid.shared.exception.UserNotFoundException;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AdminUserServiceTest {

    @Mock
    private UserQueryService userQueryService;

    @Mock
    private LoginAttemptPort loginAttemptService;

    @Mock
    private AuditService auditService;

    @InjectMocks
    private AdminUserService adminUserService;

    @Test
    void unlockAccount_unlocksUserAndLogsEvent() {
        var adminId = UUID.randomUUID();
        var targetUserId = UUID.randomUUID();
        var user = createUser(targetUserId, "K48-2024-001");

        when(userQueryService.findById(targetUserId)).thenReturn(Optional.of(user));

        adminUserService.unlockAccount(targetUserId, adminId);

        verify(loginAttemptService, times(1)).unlockAccount("K48-2024-001", adminId);
        verify(auditService, times(1)).log(eq(adminId), eq("ACCOUNT_UNLOCKED_BY_ADMIN"), any());
    }

    @Test
    void unlockAccount_throwsWhenUserNotFound() {
        var adminId = UUID.randomUUID();
        var targetUserId = UUID.randomUUID();

        when(userQueryService.findById(targetUserId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> adminUserService.unlockAccount(targetUserId, adminId))
                .isInstanceOf(UserNotFoundException.class);
    }

    private User createUser(UUID id, String matricule) {
        var role = new Role();
        role.setName("STUDENT");

        return User.builder()
                .id(id)
                .matricule(matricule)
                .email("test@k48.io")
                .name("Test User")
                .passwordHash("hash")
                .status(UserStatus.ACTIVE)
                .batch("2024")
                .specialization("SE")
                .phone("+237600000000")
                .profileCompleted(false)
                .requiresPasswordChange(false)
                .roles(Set.of(role))
                .build();
    }
}
