package io.k48.fortyeightid.operator.internal;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.Instant;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(
    name = "operator_memberships",
    uniqueConstraints = @UniqueConstraint(columnNames = {"account_id", "user_id"})
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OperatorMembership {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "account_id", nullable = false)
    private UUID accountId;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Enumerated(EnumType.STRING)
    @Column(name = "member_role", nullable = false, length = 20)
    @Builder.Default
    private MemberRole memberRole = MemberRole.COLLABORATOR;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private MembershipStatus status = MembershipStatus.PENDING;

    @Column(name = "invited_at", nullable = false, updatable = false)
    @Builder.Default
    private Instant invitedAt = Instant.now();

    @Column(name = "accepted_at")
    private Instant acceptedAt;

    public void accept() {
        this.status = MembershipStatus.ACTIVE;
        this.acceptedAt = Instant.now();
    }

    public enum MemberRole {
        OWNER, COLLABORATOR
    }

    public enum MembershipStatus {
        PENDING, ACTIVE
    }
}
