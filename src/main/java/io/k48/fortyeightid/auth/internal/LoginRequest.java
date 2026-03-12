package io.k48.fortyeightid.auth.internal;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

record LoginRequest(
        @NotBlank(message = "Matricule is required")
        @Size(max = 50, message = "Matricule must not exceed 50 characters")
        String matricule,

        @NotBlank(message = "Password is required")
        @Size(min = 1, max = 100, message = "Password must be between 1 and 100 characters")
        String password
) {}
