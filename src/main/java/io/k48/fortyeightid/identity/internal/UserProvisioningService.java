package io.k48.fortyeightid.identity.internal;

import io.k48.fortyeightid.shared.MatriculeValidator;
import io.k48.fortyeightid.shared.exception.InvalidMatriculeFormatException;
import io.k48.fortyeightid.identity.Role;
import io.k48.fortyeightid.identity.User;
import io.k48.fortyeightid.identity.UserProvisioningPort;
import io.k48.fortyeightid.identity.UserStatus;
import io.k48.fortyeightid.shared.exception.DuplicateEmailException;
import io.k48.fortyeightid.shared.exception.DuplicateMatriculeException;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
class UserProvisioningService implements UserProvisioningPort {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public User createUser(String matricule, String email, String name,
                           String phone, String batch, String specialization,
                           String rawPassword) {

        MatriculeValidator.validate(matricule, batch)
                .ifPresent(error -> { throw new InvalidMatriculeFormatException(error); });

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
                .roles(Set.of(studentRole))
                .build();

        return userRepository.save(user);
    }
}
