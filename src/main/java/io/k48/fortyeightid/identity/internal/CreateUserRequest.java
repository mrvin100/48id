package io.k48.fortyeightid.identity.internal;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

record CreateUserRequest(
        @NotBlank(message = "Matricule is required")
        @Size(max = 50, message = "Matricule must not exceed 50 characters")
        String matricule,

        @NotBlank(message = "Email is required")
        @Email(message = "Invalid email format")
        @Size(max = 255, message = "Email must not exceed 255 characters")
        String email,

        @NotBlank(message = "Name is required")
        @Size(max = 255, message = "Name must not exceed 255 characters")
        String name,

        @Size(max = 30, message = "Phone must not exceed 30 characters")
        String phone,

        @Size(max = 50, message = "Batch must not exceed 50 characters")
        String batch,

        @Size(max = 100, message = "Specialization must not exceed 100 characters")
        String specialization,

        @NotBlank(message = "Password is required")
        @Size(min = 8, max = 100, message = "Password must be between 8 and 100 characters")
        String password
) {}
