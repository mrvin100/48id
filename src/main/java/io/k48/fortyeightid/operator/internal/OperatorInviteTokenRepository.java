package io.k48.fortyeightid.operator.internal;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

interface OperatorInviteTokenRepository extends JpaRepository<OperatorInviteToken, UUID> {

    Optional<OperatorInviteToken> findByToken(String token);

    void deleteAllByUserId(UUID userId);
}
