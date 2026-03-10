package io.k48.fortyeightid.auth.internal;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

record CreateApiKeyRequest(
        @NotBlank(message = "Application name is required")
        @Size(max = 100, message = "Application name must not exceed 100 characters")
        String applicationName,

        @Size(max = 500, message = "Description must not exceed 500 characters")
        String description) {
}
