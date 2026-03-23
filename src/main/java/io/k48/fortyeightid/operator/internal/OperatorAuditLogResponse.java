package io.k48.fortyeightid.operator.internal;

import java.time.Instant;
import java.util.UUID;

record OperatorAuditLogResponse(
        UUID id,
        UUID userId,
        String action,
        String ipAddress,
        String userAgent,
        Instant timestamp
) {}
