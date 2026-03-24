package io.k48.fortyeightid.auth.internal;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonInclude(JsonInclude.Include.NON_NULL)
record VerifyTokenResponse(
        @JsonProperty("valid") boolean valid,
        @JsonProperty("user") UserInfo user,
        @JsonProperty("reason") String reason
) {
    public static VerifyTokenResponse valid(UserInfo user) {
        return new VerifyTokenResponse(true, user, null);
    }

    public static VerifyTokenResponse invalid(String reason) {
        return new VerifyTokenResponse(false, null, reason);
    }

    record UserInfo(
            @JsonProperty("id") String id,
            @JsonProperty("matricule") String matricule,
            @JsonProperty("name") String name,
            @JsonProperty("email") String email,
            @JsonProperty("role") String role,
            @JsonProperty("batch") String batch,
            @JsonProperty("specialization") String specialization
    ) {}
}
