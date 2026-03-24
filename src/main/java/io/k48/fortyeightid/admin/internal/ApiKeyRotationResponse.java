package io.k48.fortyeightid.admin.internal;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.Instant;

record ApiKeyRotationResponse(
        @JsonProperty("key") String key,
        @JsonProperty("applicationName") String applicationName,
        @JsonProperty("rotatedAt") Instant rotatedAt
) {
}
