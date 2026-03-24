package io.k48.fortyeightid.admin.internal;

import jakarta.validation.constraints.NotBlank;

record ChangeRoleRequest(
        @NotBlank String role
) {}
