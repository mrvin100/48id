package io.k48.fortyeightid.auth.internal;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OperatorInviteTokenRepository extends JpaRepository<OperatorInviteToken, UUID> {

    Optional<OperatorInviteToken> findByToken(String token);

    void deleteAllByUserId(UUID userId);
}
