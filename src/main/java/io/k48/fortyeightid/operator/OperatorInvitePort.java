package io.k48.fortyeightid.operator;

import java.util.UUID;

public interface OperatorInvitePort {

    void activateMembership(UUID membershipId);

    boolean hasMembershipWithStatus(UUID userId, String status);

    boolean isOwnerOfAccount(UUID userId, UUID accountId);
}
