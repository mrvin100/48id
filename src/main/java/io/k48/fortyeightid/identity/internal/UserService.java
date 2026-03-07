package io.k48.fortyeightid.identity.internal;

import io.k48.fortyeightid.identity.User;
import io.k48.fortyeightid.identity.UserStatus;
import io.k48.fortyeightid.shared.exception.DuplicateEmailException;
import io.k48.fortyeightid.shared.exception.DuplicateMatriculeException;
import io.k48.fortyeightid.shared.exception.UserNotFoundException;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    User createUser(CreateUserRequest request) {
        if (userRepository.existsByMatricule(request.matricule())) {
            throw new DuplicateMatriculeException(request.matricule());
        }
        if (userRepository.existsByEmail(request.email())) {
            throw new DuplicateEmailException(request.email());
        }

        var user = User.builder()
                .matricule(request.matricule())
                .email(request.email())
                .name(request.name())
                .phone(request.phone())
                .batch(request.batch())
                .specialization(request.specialization())
                .passwordHash(passwordEncoder.encode(request.password()))
                .status(UserStatus.PENDING_ACTIVATION)
                .profileCompleted(false)
                .build();

        return userRepository.save(user);
    }

    User findById(UUID id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new UserNotFoundException("User not found: " + id));
    }

    User findByMatricule(String matricule) {
        return userRepository.findByMatricule(matricule)
                .orElseThrow(() -> new UserNotFoundException("User not found: " + matricule));
    }

    Page<User> findByStatus(UserStatus status, Pageable pageable) {
        return userRepository.findByStatus(status, pageable);
    }

    @Transactional
    User updateStatus(UUID id, UserStatus status) {
        var user = findById(id);
        user.setStatus(status);
        return userRepository.save(user);
    }
}
