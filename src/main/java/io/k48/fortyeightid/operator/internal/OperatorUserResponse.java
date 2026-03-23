package io.k48.fortyeightid.operator.internal;

import io.k48.fortyeightid.identity.User;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

record OperatorUserResponse(
        UUID id,
        String matricule,
        String email,
        String name,
        String batch,
        String specialization,
        String status,
        Set<String> roles,
        OffsetDateTime lastLoginAt,
        Instant createdAt
) {
    static OperatorUserResponse from(User user) {
        return new OperatorUserResponse(
                user.getId(),
                user.getMatricule(),
                user.getEmail(),
                user.getName(),
                user.getBatch(),
                user.getSpecialization(),
                user.getStatus().name(),
                user.getRoles().stream().map(r -> r.getName()).collect(Collectors.toSet()),
                user.getLastLoginAt(),
                user.getCreatedAt()
        );
    }
}
