package io.k48.fortyeightid.identity;

import io.k48.fortyeightid.identity.internal.UserRepository;
import io.k48.fortyeightid.shared.exception.UserNotFoundException;
import jakarta.persistence.criteria.Predicate;
import java.util.ArrayList;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserQueryService {

    private final UserRepository userRepository;

    public Optional<User> findByMatricule(String matricule) {
        return userRepository.findByMatricule(matricule);
    }

    public Optional<User> findById(UUID id) {
        return userRepository.findById(id);
    }

    public Optional<User> findByEmail(String email) {
        return userRepository.findByEmail(email);
    }

    public User getByMatricule(String matricule) {
        return userRepository.findByMatricule(matricule)
                .orElseThrow(() -> new UserNotFoundException("User not found: " + matricule));
    }

    public Page<User> findAll(UserStatus status, String batch, String role, Pageable pageable) {
        Specification<User> spec = (root, query, cb) -> {
            var predicates = new ArrayList<Predicate>();
            if (status != null) {
                predicates.add(cb.equal(root.get("status"), status));
            }
            if (batch != null) {
                predicates.add(cb.equal(root.get("batch"), batch));
            }
            if (role != null) {
                var rolesJoin = root.join("roles");
                predicates.add(cb.equal(rolesJoin.get("name"), role));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };
        return userRepository.findAll(spec, pageable);
    }

    public User save(User user) {
        return userRepository.save(user);
    }

    public long count() {
        return userRepository.count();
    }

    public long countByStatus(UserStatus status) {
        return userRepository.countByStatus(status);
    }

    public long countByRolesContaining(Role role) {
        return userRepository.countByRolesContaining(role);
    }

    public boolean existsByMatricule(String matricule) {
        return userRepository.existsByMatricule(matricule);
    }

    public boolean existsByEmail(String email) {
        return userRepository.existsByEmail(email);
    }
}
