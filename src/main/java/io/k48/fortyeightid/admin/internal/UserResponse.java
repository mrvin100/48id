package io.k48.fortyeightid.admin.internal;

import io.k48.fortyeightid.identity.User;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

record UserResponse(
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
        OffsetDateTime lastLoginAt,
        Instant createdAt,
        Instant updatedAt
) {
    static UserResponse from(User user) {
        return new UserResponse(
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
                user.getLastLoginAt(),
                user.getCreatedAt(),
                user.getUpdatedAt()
        );
    }
}
