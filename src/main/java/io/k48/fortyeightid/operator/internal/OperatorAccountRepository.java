package io.k48.fortyeightid.operator.internal;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

interface OperatorAccountRepository extends JpaRepository<OperatorAccount, UUID> {

    Optional<OperatorAccount> findByName(String name);

    boolean existsByName(String name);
}
