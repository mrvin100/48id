package io.k48.fortyeightid.operator.internal;

import io.k48.fortyeightid.operator.internal.OperatorMembership.MemberRole;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

interface OperatorMembershipRepository extends JpaRepository<OperatorMembership, UUID> {

    List<OperatorMembership> findByAccountId(UUID accountId);

    List<OperatorMembership> findByUserId(UUID userId);

    Optional<OperatorMembership> findByAccountIdAndUserId(UUID accountId, UUID userId);

    boolean existsByAccountIdAndUserId(UUID accountId, UUID userId);

    List<OperatorMembership> findByAccountIdAndMemberRole(UUID accountId, MemberRole memberRole);
}
