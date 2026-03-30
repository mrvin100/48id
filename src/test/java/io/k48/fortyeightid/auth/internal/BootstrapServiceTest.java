package io.k48.fortyeightid.auth.internal;

import io.k48.fortyeightid.identity.Role;
import io.k48.fortyeightid.identity.User;
import io.k48.fortyeightid.identity.UserStatus;
import io.k48.fortyeightid.identity.UserQueryService;
import io.k48.fortyeightid.identity.RoleQueryService;
import io.k48.fortyeightid.shared.exception.DuplicateEmailException;
import io.k48.fortyeightid.shared.exception.DuplicateMatriculeException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BootstrapServiceTest {

    @Mock
    private UserQueryService userQueryService;

    @Mock
    private RoleQueryService roleQueryService;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private BootstrapService bootstrapService;

    @Test
    void createFirstAdmin_successfullyCreatesAdmin() {
        var request = new BootstrapRequest(
                "K48-B1-1",
                "admin@k48.io",
                "Admin User",
                "SecurePass123",
                "+237600000001",
                "B1",
                "Computer Science"
        );

        var adminRole = new Role();
        adminRole.setName("ADMIN");

        var savedUser = User.builder()
                .id(UUID.randomUUID())
                .matricule(request.matricule())
                .email(request.email())
                .name(request.name())
                .status(UserStatus.ACTIVE)
                .roles(java.util.Set.of(adminRole))
                .build();

        when(roleQueryService.findByName("ADMIN")).thenReturn(java.util.Optional.of(adminRole));
        when(userQueryService.countByRolesContaining(adminRole)).thenReturn(0L);
        when(userQueryService.existsByMatricule(request.matricule())).thenReturn(false);
        when(userQueryService.existsByEmail(request.email())).thenReturn(false);
        when(passwordEncoder.encode(request.password())).thenReturn("hashedPassword");
        when(userQueryService.save(any(User.class))).thenReturn(savedUser);

        var response = bootstrapService.createFirstAdmin(request);

        assertThat(response.message()).contains("First admin user created successfully");
        assertThat(response.matricule()).isEqualTo(request.matricule());
        assertThat(response.email()).isEqualTo(request.email());
        assertThat(response.userId()).isEqualTo(savedUser.getId().toString());
    }

    @Test
    void createFirstAdmin_throwsWhenAdminAlreadyExists() {
        var request = new BootstrapRequest(
                "K48-B1-1",
                "admin@k48.io",
                "Admin User",
                "SecurePass123",
                null,
                null,
                null
        );

        var adminRole = new Role();
        adminRole.setName("ADMIN");

        when(roleQueryService.findByName("ADMIN")).thenReturn(java.util.Optional.of(adminRole));
        when(userQueryService.countByRolesContaining(adminRole)).thenReturn(1L);

        assertThatThrownBy(() -> bootstrapService.createFirstAdmin(request))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("admin users already exist");
    }

    @Test
    void createFirstAdmin_throwsWhenMatriculeExists() {
        var request = new BootstrapRequest(
                "K48-B1-1",
                "admin@k48.io",
                "Admin User",
                "SecurePass123",
                null,
                null,
                null
        );

        var adminRole = new Role();
        adminRole.setName("ADMIN");

        when(roleQueryService.findByName("ADMIN")).thenReturn(java.util.Optional.of(adminRole));
        when(userQueryService.countByRolesContaining(adminRole)).thenReturn(0L);
        when(userQueryService.existsByMatricule(request.matricule())).thenReturn(true);

        assertThatThrownBy(() -> bootstrapService.createFirstAdmin(request))
                .isInstanceOf(DuplicateMatriculeException.class)
                .hasMessageContaining("Matricule already exists");
    }

    @Test
    void createFirstAdmin_throwsWhenEmailExists() {
        var request = new BootstrapRequest(
                "K48-B1-1",
                "admin@k48.io",
                "Admin User",
                "SecurePass123",
                null,
                null,
                null
        );

        var adminRole = new Role();
        adminRole.setName("ADMIN");

        when(roleQueryService.findByName("ADMIN")).thenReturn(java.util.Optional.of(adminRole));
        when(userQueryService.countByRolesContaining(adminRole)).thenReturn(0L);
        when(userQueryService.existsByMatricule(request.matricule())).thenReturn(false);
        when(userQueryService.existsByEmail(request.email())).thenReturn(true);

        assertThatThrownBy(() -> bootstrapService.createFirstAdmin(request))
                .isInstanceOf(DuplicateEmailException.class)
                .hasMessageContaining("Email already exists");
    }

    @Test
    void isBootstrapAvailable_returnsTrueWhenNoAdminExists() {
        var adminRole = new Role();
        adminRole.setName("ADMIN");

        when(roleQueryService.findByName("ADMIN")).thenReturn(Optional.of(adminRole));
        when(userQueryService.countByRolesContaining(adminRole)).thenReturn(0L);

        assertThat(bootstrapService.isBootstrapAvailable()).isTrue();
    }

    @Test
    void isBootstrapAvailable_returnsFalseWhenAdminExists() {
        var adminRole = new Role();
        adminRole.setName("ADMIN");

        when(roleQueryService.findByName("ADMIN")).thenReturn(Optional.of(adminRole));
        when(userQueryService.countByRolesContaining(adminRole)).thenReturn(1L);

        assertThat(bootstrapService.isBootstrapAvailable()).isFalse();
    }

    @Test
    void isBootstrapAvailable_returnsFalseWhenAdminRoleNotFound() {
        when(roleQueryService.findByName("ADMIN")).thenReturn(Optional.empty());

        assertThat(bootstrapService.isBootstrapAvailable()).isFalse();
    }
}