package io.k48.fortyeightid.identity.internal;

import jakarta.validation.constraints.Size;

record UpdateProfileRequest(
        @Size(max = 30, message = "Phone must not exceed 30 characters")
        String phone,

        @Size(max = 100, message = "Specialization must not exceed 100 characters")
        String specialization
) {
}
