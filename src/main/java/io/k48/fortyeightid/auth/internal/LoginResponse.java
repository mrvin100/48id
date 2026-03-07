package io.k48.fortyeightid.auth.internal;

import com.fasterxml.jackson.annotation.JsonProperty;

record LoginResponse(
        @JsonProperty("access_token") String accessToken,
        @JsonProperty("refresh_token") String refreshToken,
        @JsonProperty("token_type") String tokenType,
        @JsonProperty("expires_in") long expiresIn,
        UserInfo user
) {
    record UserInfo(
            String id,
            String matricule,
            String name,
            String role,
            String batch,
            String specialization
    ) {}
}
