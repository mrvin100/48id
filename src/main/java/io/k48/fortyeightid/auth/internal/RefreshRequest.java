package io.k48.fortyeightid.auth.internal;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;

record RefreshRequest(
        @NotBlank @JsonProperty("refresh_token") String refreshToken
) {}
