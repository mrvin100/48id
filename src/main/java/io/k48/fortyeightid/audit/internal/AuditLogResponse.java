package io.k48.fortyeightid.audit.internal;

import java.time.Instant;
import java.util.UUID;

public record AuditLogResponse(
        UUID id,
        String eventType,
        UUID userId,
        String ipAddress,
        Instant timestamp,
        String metadata) {

    public static AuditLogResponse from(AuditLog auditLog) {
        return new AuditLogResponse(
                auditLog.getId(),
                auditLog.getAction(),
                auditLog.getUserId(),
                auditLog.getIpAddress(),
                auditLog.getCreatedAt(),
                auditLog.getDetails());
    }
}
