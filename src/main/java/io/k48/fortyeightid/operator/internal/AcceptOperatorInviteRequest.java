package io.k48.fortyeightid.operator.internal;

import jakarta.validation.constraints.NotBlank;

record AcceptOperatorInviteRequest(@NotBlank String token) {}
