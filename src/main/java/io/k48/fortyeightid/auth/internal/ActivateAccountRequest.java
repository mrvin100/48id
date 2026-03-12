package io.k48.fortyeightid.auth.internal;

import jakarta.validation.constraints.NotBlank;

record ActivateAccountRequest(
        @NotBlank(message = "Activation token is required")
        String token
) {
}
