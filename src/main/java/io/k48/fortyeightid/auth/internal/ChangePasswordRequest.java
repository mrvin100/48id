package io.k48.fortyeightid.auth.internal;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

record ChangePasswordRequest(
        @NotBlank(message = "Current password is required")
        @Size(min = 1, message = "Current password is required")
        String currentPassword,
        
        @NotBlank(message = "New password is required")
        @Size(min = 8, message = "New password must be at least 8 characters")
        String newPassword
) {
}
