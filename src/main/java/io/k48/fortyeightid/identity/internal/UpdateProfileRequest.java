package io.k48.fortyeightid.identity.internal;

import jakarta.validation.constraints.NotBlank;

record UpdateProfileRequest(
        String phone,
        String specialization
) {
}
