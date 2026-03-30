package io.k48.fortyeightid.identity.internal;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.k48.fortyeightid.identity.Role;
import io.k48.fortyeightid.shared.exception.InvalidMatriculeFormatException;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
class UserProvisioningServiceMatriculeTest {

    @Mock private UserRepository userRepository;
    @Mock private RoleRepository roleRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @InjectMocks private UserProvisioningService userProvisioningService;

    @Test
    void createUser_invalidMatriculeFormat_throwsInvalidMatriculeFormatException() {
        assertThatThrownBy(() ->
                userProvisioningService.createUser("K48-2024-001", "a@k48.io", "A", "+1", "B1", "SE", "pass"))
                .isInstanceOf(InvalidMatriculeFormatException.class)
                .hasMessageContaining("does not match required format");

        verify(userRepository, never()).save(any());
    }

    @Test
    void createUser_prefixMismatch_throwsWithExactMessage() {
        assertThatThrownBy(() ->
                userProvisioningService.createUser("K48-B2-5", "a@k48.io", "A", "+1", "B1", "SE", "pass"))
                .isInstanceOf(InvalidMatriculeFormatException.class)
                .hasMessage("Matricule prefix 'K48-B2' does not match batch 'B1'");

        verify(userRepository, never()).save(any());
    }

    @Test
    void createUser_validMatricule_proceedsToRepository() {
        var role = new Role();
        role.setName("STUDENT");
        when(roleRepository.findByName("STUDENT")).thenReturn(Optional.of(role));
        when(userRepository.existsByMatricule("K48-B1-12")).thenReturn(false);
        when(userRepository.existsByEmail("a@k48.io")).thenReturn(false);
        when(passwordEncoder.encode("pass")).thenReturn("hashed");
        when(userRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        userProvisioningService.createUser("K48-B1-12", "a@k48.io", "A", "+1", "B1", "SE", "pass");

        verify(userRepository).save(any());
    }
}
