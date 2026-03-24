package io.k48.fortyeightid.operator;

import java.time.Instant;
import java.util.UUID;

public interface OperatorAccountPort {

    record CreateOperatorAccountCommand(String name, String description, UUID adminId) {}

    record OperatorAccountCreated(
        UUID accountId,
        String accountName,
        String accountDescription,
        UUID createdBy,
        Instant createdAt,
        UUID membershipId,
        UUID ownerId,
        String ownerMatricule,
        String ownerName
    ) {}

    OperatorAccountCreated createAccount(CreateOperatorAccountCommand command);
}
