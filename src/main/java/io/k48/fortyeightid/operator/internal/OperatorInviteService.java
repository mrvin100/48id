package io.k48.fortyeightid.operator.internal;

import io.k48.fortyeightid.operator.internal.OperatorMembership.MemberRole;
import io.k48.fortyeightid.operator.internal.OperatorMembership.MembershipStatus;
import io.k48.fortyeightid.operator.ports.OperatorInvitePort;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
class OperatorInviteService implements OperatorInvitePort {

    private static final Logger log = LoggerFactory.getLogger(OperatorInviteService.class);

    private final OperatorMembershipRepository membershipRepository;

    @Override
    @Transactional
    public void activateMembership(UUID membershipId) {
        membershipRepository.findById(membershipId).ifPresentOrElse(
            m -> {
                m.accept();
                membershipRepository.save(m);
                log.info("Membership {} activated for user {}", membershipId, m.getUserId());
            },
            () -> log.warn("activateMembership called with unknown membershipId={}", membershipId)
        );
    }

    @Override
    public boolean hasMembershipWithStatus(UUID userId, String status) {
        MembershipStatus membershipStatus = MembershipStatus.valueOf(status);
        return membershipRepository.findByUserId(userId).stream()
            .anyMatch(m -> m.getStatus() == membershipStatus);
    }

    @Override
    public boolean isOwnerOfAccount(UUID userId, UUID accountId) {
        return membershipRepository.findByAccountIdAndUserId(accountId, userId)
            .map(m -> m.getMemberRole() == MemberRole.OWNER)
            .orElse(false);
    }
}
