package io.k48.fortyeightid.operator.internal;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

interface OperatorAccountRepository extends JpaRepository<OperatorAccount, UUID> {
}
