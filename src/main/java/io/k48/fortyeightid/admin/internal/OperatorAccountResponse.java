package io.k48.fortyeightid.admin.internal;

import io.k48.fortyeightid.operator.OperatorAccountPort.OperatorAccountCreated;
import java.time.Instant;
import java.util.UUID;

record OperatorAccountResponse(
    UUID id,
    String name,
    String description,
    UUID createdBy,
    Instant createdAt,
    OwnerInfo owner
) {
    record OwnerInfo(UUID userId, String matricule, String name) {}

    static OperatorAccountResponse from(OperatorAccountCreated created) {
        return new OperatorAccountResponse(
            created.accountId(),
            created.accountName(),
            created.accountDescription(),
            created.createdBy(),
            created.createdAt(),
            new OwnerInfo(created.ownerId(), created.ownerMatricule(), created.ownerName())
        );
    }
}
