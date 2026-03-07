package io.k48.fortyeightid.identity.internal;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

interface UserRepository extends JpaRepository<User, UUID> {

    Optional<User> findByMatricule(String matricule);

    Optional<User> findByEmail(String email);

    boolean existsByMatricule(String matricule);

    boolean existsByEmail(String email);

    Page<User> findByStatus(UserStatus status, Pageable pageable);

    Page<User> findByBatch(String batch, Pageable pageable);
}
