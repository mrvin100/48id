package io.k48.fortyeightid.auth.internal;

import jakarta.validation.constraints.NotBlank;

record LoginRequest(
        @NotBlank String matricule,
        @NotBlank String password
) {}
