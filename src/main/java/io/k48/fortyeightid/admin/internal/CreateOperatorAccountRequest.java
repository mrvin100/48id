package io.k48.fortyeightid.admin.internal;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

record CreateOperatorAccountRequest(
    @NotBlank @Size(max = 100) String name,
    @Size(max = 500) String description
) {}
