package io.k48.fortyeightid.identity;

import io.k48.fortyeightid.identity.internal.RoleRepository;
import io.k48.fortyeightid.identity.internal.UserRepository;
import io.k48.fortyeightid.shared.exception.DuplicateEmailException;
import io.k48.fortyeightid.shared.exception.DuplicateMatriculeException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

/**
 * Public facade for cross-module user provisioning (e.g. CSV import).
 */
@Service
@RequiredArgsConstructor
public class UserProvisioningService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;

    /**
     * Creates a new user account with PENDING_ACTIVATION status.
     *
     * @throws DuplicateMatriculeException if matricule already exists
     * @throws DuplicateEmailException     if email already exists
     */
    public User createUser(String matricule, String email, String name,
                           String phone, String batch, String specialization,
                           String rawPassword) {
        
        if (userRepository.existsByMatricule(matricule)) {
            throw new DuplicateMatriculeException("Matricule already exists: " + matricule);
        }
        if (userRepository.existsByEmail(email)) {
            throw new DuplicateEmailException("Email already exists: " + email);
        }

        var studentRole = roleRepository.findByName("STUDENT")
                .orElseThrow(() -> new IllegalStateException("STUDENT role not found"));

        var user = User.builder()
                .matricule(matricule)
                .email(email)
                .name(name)
                .phone(phone)
                .batch(batch)
                .specialization(specialization)
                .passwordHash(passwordEncoder.encode(rawPassword))
                .status(UserStatus.PENDING_ACTIVATION)
                .roles(java.util.Set.of(studentRole))
                .build();

        return userRepository.save(user);
    }
}
