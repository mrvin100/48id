package io.k48.fortyeightid.operator.internal;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;

record AcceptOperatorInviteRequest(
        @NotBlank String token,
        @NotNull UUID accountId
) {}
