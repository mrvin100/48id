package io.k48.fortyeightid.auth.internal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import io.k48.fortyeightid.identity.Role;
import io.k48.fortyeightid.identity.User;
import io.k48.fortyeightid.identity.UserQueryService;
import io.k48.fortyeightid.identity.UserStatus;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

@ExtendWith(MockitoExtension.class)
class UserDetailsServiceImplTest {

    @Mock
    private UserQueryService userQueryService;

    @InjectMocks
    private UserDetailsServiceImpl userDetailsService;

    @Test
    void loadUserByUsername_returnsUserPrincipal() {
        var user = User.builder()
                .matricule("K48-2024-001")
                .email("test@k48.io")
                .name("Test")
                .passwordHash("$2a$10$hash")
                .status(UserStatus.ACTIVE)
                .build();
        when(userQueryService.findByMatricule("K48-2024-001")).thenReturn(Optional.of(user));

        var principal = userDetailsService.loadUserByUsername("K48-2024-001");

        assertThat(principal.getUsername()).isEqualTo("K48-2024-001");
        assertThat(principal.isEnabled()).isTrue();
    }

    @Test
    void loadUserByUsername_throwsWhenNotFound() {
        when(userQueryService.findByMatricule("NONE")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userDetailsService.loadUserByUsername("NONE"))
                .isInstanceOf(UsernameNotFoundException.class);
    }

    @Test
    void loadUserByUsername_throwsWhenSuspended() {
        var user = User.builder()
                .matricule("K48-2024-002")
                .email("sus@k48.io")
                .name("Suspended")
                .passwordHash("$2a$10$hash")
                .status(UserStatus.SUSPENDED)
                .build();
        when(userQueryService.findByMatricule("K48-2024-002")).thenReturn(Optional.of(user));

        assertThatThrownBy(() -> userDetailsService.loadUserByUsername("K48-2024-002"))
                .isInstanceOf(DisabledException.class);
    }
}
