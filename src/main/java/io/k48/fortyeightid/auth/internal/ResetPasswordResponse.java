package io.k48.fortyeightid.auth.internal;

import com.fasterxml.jackson.annotation.JsonProperty;

record ResetPasswordResponse(
        @JsonProperty("message") String message
) {
}
