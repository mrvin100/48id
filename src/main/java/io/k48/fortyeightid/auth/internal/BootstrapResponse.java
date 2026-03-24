package io.k48.fortyeightid.auth.internal;

/**
 * Response for successful bootstrap admin creation.
 */
record BootstrapResponse(
        String message,
        String userId,
        String matricule,
        String email
) {}