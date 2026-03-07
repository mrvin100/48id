package io.k48.fortyeightid.identity.internal;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

record CreateUserRequest(
        @NotBlank @Size(max = 50) String matricule,
        @NotBlank @Email String email,
        @NotBlank String name,
        String phone,
        String batch,
        String specialization,
        @NotBlank @Size(min = 8) String password
) {}
