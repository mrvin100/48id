package io.k48.fortyeightid.auth.internal;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Request to create the first admin user when no admin exists in the system.
 * This is a bootstrap operation that can only be performed once.
 */
record BootstrapRequest(
        @NotBlank(message = "Matricule is required")
        @Pattern(regexp = "^K48-\\d{4}-\\d{3}$", message = "Matricule must follow format K48-YYYY-XXX")
        String matricule,

        @NotBlank(message = "Email is required")
        @Email(message = "Email must be valid")
        String email,

        @NotBlank(message = "Name is required")
        @Size(min = 2, max = 100, message = "Name must be between 2 and 100 characters")
        String name,

        @NotBlank(message = "Password is required")
        @Size(min = 8, max = 128, message = "Password must be between 8 and 128 characters")
        @Pattern(regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d).*$", 
                message = "Password must contain at least one lowercase letter, one uppercase letter, and one digit")
        String password,

        String phone,
        String batch,
        String specialization
) {}