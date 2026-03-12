package io.k48.fortyeightid.admin.internal;

import java.time.Instant;
import java.util.UUID;

record AuditLogResponse(
        UUID id,
        UUID userId,
        String action,
        String ipAddress,
        String userAgent,
        Instant timestamp
) {
}
