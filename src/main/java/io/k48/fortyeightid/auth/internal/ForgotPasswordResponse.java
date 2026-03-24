package io.k48.fortyeightid.auth.internal;

import com.fasterxml.jackson.annotation.JsonProperty;

record ForgotPasswordResponse(
        @JsonProperty("message") String message
) {
}
