package io.k48.fortyeightid.identity.internal;

import io.k48.fortyeightid.identity.User;
import java.time.Instant;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

record MeResponse(
        UUID id,
        String matricule,
        String email,
        String name,
        String phone,
        String batch,
        String specialization,
        String status,
        Set<String> roles,
        boolean profileCompleted,
        Instant createdAt,
        Instant updatedAt
) {
    static MeResponse from(User user) {
        return new MeResponse(
                user.getId(),
                user.getMatricule(),
                user.getEmail(),
                user.getName(),
                user.getPhone(),
                user.getBatch(),
                user.getSpecialization(),
                user.getStatus().name(),
                user.getRoles().stream().map(r -> r.getName()).collect(Collectors.toSet()),
                user.isProfileCompleted(),
                user.getCreatedAt(),
                user.getUpdatedAt()
        );
    }
}
