package io.k48.fortyeightid.auth.internal;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonInclude(JsonInclude.Include.NON_NULL)
record PublicIdentityResponse(
        @JsonProperty("id") String id,
        @JsonProperty("matricule") String matricule,
        @JsonProperty("name") String name,
        @JsonProperty("batch") String batch,
        @JsonProperty("specialization") String specialization,
        @JsonProperty("profileCompleted") boolean profileCompleted
) {
}
