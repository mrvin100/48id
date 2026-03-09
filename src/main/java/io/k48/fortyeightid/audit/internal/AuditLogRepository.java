package io.k48.fortyeightid.audit.internal;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AuditLogRepository extends JpaRepository<AuditLog, UUID> {
}
