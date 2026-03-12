package io.k48.fortyeightid.admin.internal;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.Instant;
import java.util.UUID;

record AuditLogResponse(
        @JsonProperty("id") UUID id,
        @JsonProperty("userId") UUID userId,
        @JsonProperty("action") String action,
        @JsonProperty("ipAddress") String ipAddress,
        @JsonProperty("userAgent") String userAgent,
        @JsonProperty("timestamp") Instant timestamp
) {
}
