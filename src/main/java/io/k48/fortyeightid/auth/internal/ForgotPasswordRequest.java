package io.k48.fortyeightid.auth.internal;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

record ForgotPasswordRequest(
        @NotBlank(message = "Email is required")
        @Email(message = "Invalid email format")
        String email
) {
}
