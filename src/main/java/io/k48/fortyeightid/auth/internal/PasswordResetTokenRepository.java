package io.k48.fortyeightid.auth.internal;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

interface PasswordResetTokenRepository extends JpaRepository<PasswordResetToken, UUID> {

    @Modifying
    @Query("DELETE FROM PasswordResetToken t WHERE t.userId = :userId")
    void deleteAllByUserId(UUID userId);
}
