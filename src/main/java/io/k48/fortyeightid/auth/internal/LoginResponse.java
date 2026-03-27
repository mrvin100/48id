package io.k48.fortyeightid.auth.internal;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

record LoginResponse(
        @JsonProperty("access_token") String accessToken,
        @JsonProperty("refresh_token") String refreshToken,
        @JsonProperty("token_type") String tokenType,
        @JsonProperty("expires_in") long expiresIn,
        @JsonProperty("requires_password_change") boolean requiresPasswordChange,
        UserInfo user
) {
    record UserInfo(
            String id,
            String matricule,
            String email,
            String name,
            List<String> roles,
            String batch,
            String specialization,
            String status,
            @JsonProperty("profile_completed") boolean profileCompleted,
            @JsonProperty("last_login_at") java.time.OffsetDateTime lastLoginAt,
            @JsonProperty("created_at") java.time.Instant createdAt,
            @JsonProperty("updated_at") java.time.Instant updatedAt
    ) {}
}
