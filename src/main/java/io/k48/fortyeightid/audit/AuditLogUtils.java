package io.k48.fortyeightid.audit;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;

/**
 * Shared utility for extracting fields from AuditLog JSON details.
 */
@Slf4j
public final class AuditLogUtils {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private AuditLogUtils() {}

    public static String extractField(AuditLog auditLog, String field) {
        try {
            var node = MAPPER.readTree(auditLog.getDetails());
            var value = node.get(field);
            return value != null ? value.asText() : null;
        } catch (Exception e) {
            log.debug("Failed to parse audit log details for field '{}', details='{}': {}",
                    field, auditLog.getDetails(), e.getMessage());
            return null;
        }
    }
}
