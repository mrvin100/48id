package io.k48.fortyeightid.auth.internal;

import com.fasterxml.jackson.annotation.JsonProperty;

record ActivateAccountResponse(
        @JsonProperty("message") String message
) {
}
