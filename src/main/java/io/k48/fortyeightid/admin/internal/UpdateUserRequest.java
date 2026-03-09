package io.k48.fortyeightid.admin.internal;

import com.fasterxml.jackson.annotation.JsonProperty;

record UpdateUserRequest(
        String matricule,
        String email,
        String name,
        String phone,
        String batch,
        String specialization
) {}
