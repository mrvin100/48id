package io.k48.fortyeightid.operator.internal;

import static org.assertj.core.api.Assertions.assertThat;

import io.k48.fortyeightid.operator.internal.OperatorMembership.MemberRole;
import io.k48.fortyeightid.operator.internal.OperatorMembership.MembershipStatus;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class OperatorMembershipTest {

    @Test
    void build_shouldDefaultToCollaboratorAndPending() {
        var membership = OperatorMembership.builder()
            .accountId(UUID.randomUUID())
            .userId(UUID.randomUUID())
            .build();

        assertThat(membership.getMemberRole()).isEqualTo(MemberRole.COLLABORATOR);
        assertThat(membership.getStatus()).isEqualTo(MembershipStatus.PENDING);
        assertThat(membership.getInvitedAt()).isNotNull();
        assertThat(membership.getAcceptedAt()).isNull();
    }

    @Test
    void accept_shouldTransitionToActiveAndSetAcceptedAt() {
        var membership = OperatorMembership.builder()
            .accountId(UUID.randomUUID())
            .userId(UUID.randomUUID())
            .build();

        membership.accept();

        assertThat(membership.getStatus()).isEqualTo(MembershipStatus.ACTIVE);
        assertThat(membership.getAcceptedAt()).isNotNull();
    }

    @Test
    void build_shouldAllowOwnerRole_whenExplicitlySet() {
        var membership = OperatorMembership.builder()
            .accountId(UUID.randomUUID())
            .userId(UUID.randomUUID())
            .memberRole(MemberRole.OWNER)
            .build();

        assertThat(membership.getMemberRole()).isEqualTo(MemberRole.OWNER);
    }
}
