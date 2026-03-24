package io.k48.fortyeightid.admin.internal;

import jakarta.validation.constraints.NotBlank;

record ChangeStatusRequest(
        @NotBlank String status
) {}
