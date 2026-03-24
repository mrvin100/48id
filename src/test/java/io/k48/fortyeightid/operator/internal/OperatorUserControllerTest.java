package io.k48.fortyeightid.operator.internal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.k48.fortyeightid.admin.DashboardQueryPort;
import io.k48.fortyeightid.identity.Role;
import io.k48.fortyeightid.identity.User;
import io.k48.fortyeightid.identity.UserStatus;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;

@ExtendWith(MockitoExtension.class)
class OperatorUserControllerTest {

    @Mock
    private DashboardQueryPort dashboardQueryPort;

    @InjectMocks
    private OperatorUserController operatorUserController;

    @Test
    void listUsers_returnsPaginatedUsers() {
        // Given: Users exist
        var user = buildUser(UUID.randomUUID(), "K48-B1-1");
        var page = new PageImpl<>(List.of(user));
        when(dashboardQueryPort.listUsers(any(), any(), any(), any(Pageable.class))).thenReturn(page);

        // When: Operator lists users
        ResponseEntity<Page<OperatorUserResponse>> response = operatorUserController.listUsers(
                null, null, null, PageRequest.of(0, 20));

        // Then: Returns paginated users with 200 OK
        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getContent()).hasSize(1);
        assertThat(response.getBody().getContent().get(0).matricule()).isEqualTo("K48-B1-1");
        verify(dashboardQueryPort, times(1)).listUsers(any(), any(), any(), any(Pageable.class));
    }

    @Test
    void listUsers_withStatusFilter_passesFilterToPort() {
        // Given: Only active users
        var page = new PageImpl<>(List.of(buildUser(UUID.randomUUID(), "K48-B1-2")));
        when(dashboardQueryPort.listUsers(eq(UserStatus.ACTIVE), any(), any(), any(Pageable.class)))
                .thenReturn(page);

        // When: Operator filters by ACTIVE status
        ResponseEntity<Page<OperatorUserResponse>> response = operatorUserController.listUsers(
                UserStatus.ACTIVE, null, null, PageRequest.of(0, 20));

        // Then: Filter is forwarded to port
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getContent()).hasSize(1);
        verify(dashboardQueryPort, times(1)).listUsers(eq(UserStatus.ACTIVE), any(), any(), any(Pageable.class));
    }

    @Test
    void getUser_returnsUserById() {
        // Given: A user exists
        var userId = UUID.randomUUID();
        var user = buildUser(userId, "K48-B1-3");
        when(dashboardQueryPort.getUser(userId)).thenReturn(user);

        // When: Operator fetches user by ID
        ResponseEntity<OperatorUserResponse> response = operatorUserController.getUser(userId);

        // Then: Returns user with 200 OK
        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().id()).isEqualTo(userId);
        assertThat(response.getBody().matricule()).isEqualTo("K48-B1-3");
        verify(dashboardQueryPort, times(1)).getUser(userId);
    }

    private User buildUser(UUID id, String matricule) {
        var role = new Role();
        role.setName("STUDENT");
        return User.builder()
                .id(id)
                .matricule(matricule)
                .email(matricule + "@k48.io")
                .name("Test User")
                .passwordHash("hash")
                .status(UserStatus.ACTIVE)
                .batch("B1")
                .specialization("SE")
                .roles(Set.of(role))
                .build();
    }
}
