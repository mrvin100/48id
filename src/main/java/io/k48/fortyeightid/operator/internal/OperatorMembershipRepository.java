package io.k48.fortyeightid.operator.internal;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

interface OperatorMembershipRepository extends JpaRepository<OperatorMembership, UUID> {

    List<OperatorMembership> findAllByOperatorAccountId(UUID operatorAccountId);

    List<OperatorMembership> findAllByUserId(UUID userId);

    Optional<OperatorMembership> findByOperatorAccountIdAndUserId(UUID operatorAccountId, UUID userId);

    Optional<OperatorMembership> findByOperatorAccountIdAndUserIdAndStatus(
            UUID operatorAccountId, UUID userId, OperatorMemberStatus status);

    void deleteByOperatorAccountIdAndUserId(UUID operatorAccountId, UUID userId);
}
