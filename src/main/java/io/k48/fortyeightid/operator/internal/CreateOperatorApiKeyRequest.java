package io.k48.fortyeightid.operator.internal;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

record CreateOperatorApiKeyRequest(
        @NotBlank @Size(max = 100) String applicationName,
        @Size(max = 500) String description
) {}
