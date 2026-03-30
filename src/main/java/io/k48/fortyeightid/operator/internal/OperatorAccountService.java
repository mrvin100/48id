package io.k48.fortyeightid.operator.internal;

import io.k48.fortyeightid.audit.AuditService;
import io.k48.fortyeightid.auth.ApiKeyManagementPort;
import io.k48.fortyeightid.auth.EmailPort;
import io.k48.fortyeightid.auth.OperatorInviteTokenPort;
import io.k48.fortyeightid.identity.UserQueryService;
import io.k48.fortyeightid.identity.UserRoleService;
import io.k48.fortyeightid.shared.exception.OperatorAccountNotFoundException;
import io.k48.fortyeightid.shared.exception.OperatorOwnershipRequiredException;
import io.k48.fortyeightid.shared.exception.UserNotFoundException;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@Slf4j
@Service
@RequiredArgsConstructor
class OperatorAccountService {

    private static final long INVITE_TTL_SECONDS = 86400; // 24h

    private final OperatorAccountRepository operatorAccountRepository;
    private final OperatorMembershipRepository operatorMembershipRepository;
    private final OperatorInviteTokenPort operatorInviteTokenPort;
    private final ApiKeyManagementPort apiKeyManagementPort;
    private final UserQueryService userQueryService;
    private final UserRoleService userRoleService;
    private final AuditService auditService;
    private final EmailPort emailPort;

    // ── Student self-service ──────────────────────────────────────────────────

    /**
     * A STUDENT creates their own OperatorAccount.
     * They become OWNER immediately (no invite needed) and gain the OPERATOR role.
     */
    @Transactional
    OperatorAccount createAccountForStudent(String name, String description, UUID studentId) {
        var account = operatorAccountRepository.save(
                OperatorAccount.builder().name(name).description(description).build());

        operatorMembershipRepository.save(OperatorMembership.builder()
                .operatorAccountId(account.getId())
                .userId(studentId)
                .memberRole(OperatorMemberRole.OWNER)
                .status(OperatorMemberStatus.ACTIVE)
                .build());

        userRoleService.addRole(studentId, "OPERATOR");

        auditService.log(studentId, "OPERATOR_ACCOUNT_CREATED",
                Map.of("accountId", account.getId().toString(), "name", name));
        return account;
    }

    /**
     * OWNER invites another student by matricule as COLLABORATOR.
     * Sends email with token. accountId is embedded in the token — not in the URL.
     */
    @Transactional
    void inviteMemberByMatricule(UUID accountId, String matricule, UUID ownerId) {
        requireOwner(accountId, ownerId);

        var invitee = userQueryService.findByMatricule(matricule)
                .orElseThrow(() -> new UserNotFoundException(
                        "No 48ID user found with matricule: " + matricule + ". Please check and try again."));

        // Self-invite guard: owner cannot invite themselves
        if (invitee.getId().equals(ownerId)) {
            throw new IllegalStateException("You cannot invite yourself to your own operator account");
        }

        // Idempotency check: handle existing membership records
        var existingMembership = operatorMembershipRepository
                .findByOperatorAccountIdAndUserId(accountId, invitee.getId());

        if (existingMembership.isPresent()) {
            var existing = existingMembership.get();
            if (existing.getStatus() == OperatorMemberStatus.ACTIVE) {
                throw new IllegalStateException("User is already an active member of this account");
            }
            if (existing.getStatus() == OperatorMemberStatus.PENDING) {
                // Re-invite: just refresh the token and resend the email — do not create a duplicate
                var rawToken = operatorInviteTokenPort.createInviteToken(invitee.getId(), accountId, INVITE_TTL_SECONDS);
                final String toEmail = invitee.getEmail();
                final String toName = invitee.getName();
                afterCommit(() -> emailPort.sendOperatorInviteEmail(toEmail, toName, rawToken));
                log.info("Re-sent operator invite to {} for account {}", invitee.getMatricule(), accountId);
                return;
            }
        }

        operatorMembershipRepository.save(OperatorMembership.builder()
                .operatorAccountId(accountId)
                .userId(invitee.getId())
                .memberRole(OperatorMemberRole.COLLABORATOR)
                .status(OperatorMemberStatus.PENDING)
                .build());

        var rawToken = operatorInviteTokenPort.createInviteToken(invitee.getId(), accountId, INVITE_TTL_SECONDS);

        final String toEmail = invitee.getEmail();
        final String toName = invitee.getName();
        afterCommit(() -> emailPort.sendOperatorInviteEmail(toEmail, toName, rawToken));

        auditService.log(ownerId, "OPERATOR_MEMBER_INVITED",
                Map.of("accountId", accountId.toString(), "invitee", invitee.getId().toString()));
    }

    /**
     * Accepts an invite. accountId comes from the token — client only sends the token.
     */
    @Transactional
    void acceptInvite(UUID userId, UUID accountId) {
        var membership = operatorMembershipRepository
                .findByOperatorAccountIdAndUserIdAndStatus(accountId, userId, OperatorMemberStatus.PENDING)
                .orElseThrow(() -> new OperatorAccountNotFoundException(
                        "No pending invite found for user " + userId + " on account " + accountId));
        membership.setStatus(OperatorMemberStatus.ACTIVE);
        operatorMembershipRepository.save(membership);
        userRoleService.addRole(userId, "OPERATOR");
        log.info("Operator invite accepted by user {} for account {}", userId, accountId);
    }

    /**
     * OWNER deletes the account. All memberships are removed.
     * Any member (including owner) who has no remaining active memberships loses the OPERATOR role.
     */
    @Transactional
    void deleteAccount(UUID accountId, UUID ownerId) {
        requireOwner(accountId, ownerId);

        var members = operatorMembershipRepository.findAllByOperatorAccountId(accountId);
        var affectedUserIds = members.stream().map(OperatorMembership::getUserId).toList();

        operatorAccountRepository.deleteById(accountId); // cascades memberships via FK

        // Revoke OPERATOR role for any member who no longer belongs to any active account
        for (UUID userId : affectedUserIds) {
            boolean stillActive = operatorMembershipRepository.findAllByUserId(userId).stream()
                    .anyMatch(m -> m.getStatus() == OperatorMemberStatus.ACTIVE);
            if (!stillActive) {
                userRoleService.removeRole(userId, "OPERATOR");
            }
        }

        auditService.log(ownerId, "OPERATOR_ACCOUNT_DELETED", Map.of("accountId", accountId.toString()));
    }

    /**
     * OWNER removes a collaborator. If they have no other active memberships, they lose OPERATOR role.
     */
    @Transactional
    void removeMember(UUID accountId, UUID targetUserId, UUID ownerId) {
        requireOwner(accountId, ownerId);

        // Verify the target is actually a member before attempting deletion
        var membership = operatorMembershipRepository
                .findByOperatorAccountIdAndUserId(accountId, targetUserId)
                .orElseThrow(() -> new IllegalStateException(
                        "No membership found for this user in the operator account"));

        // Cannot remove the OWNER
        if (membership.getMemberRole() == OperatorMemberRole.OWNER) {
            throw new IllegalStateException("Cannot remove the account owner");
        }

        operatorMembershipRepository.deleteByOperatorAccountIdAndUserId(accountId, targetUserId);

        boolean stillActive = operatorMembershipRepository.findAllByUserId(targetUserId).stream()
                .anyMatch(m -> m.getStatus() == OperatorMemberStatus.ACTIVE);
        if (!stillActive) {
            userRoleService.removeRole(targetUserId, "OPERATOR");
        }

        auditService.log(ownerId, "OPERATOR_MEMBER_REMOVED",
                Map.of("accountId", accountId.toString(), "targetUser", targetUserId.toString()));
    }

    // ── Read operations ───────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    List<StudentOperatorController.MyAccountResponse> listAccountsForUser(UUID userId) {
        return operatorMembershipRepository.findAllByUserId(userId).stream()
                .filter(m -> m.getStatus() == OperatorMemberStatus.ACTIVE)
                .flatMap(m -> operatorAccountRepository.findById(m.getOperatorAccountId())
                        .map(a -> new StudentOperatorController.MyAccountResponse(
                                a.getId(), a.getName(), a.getDescription(),
                                a.getOwnedApiKeyId(), a.getCreatedAt(),
                                m.getMemberRole().name(), m.getStatus().name()))
                        .stream())
                .toList();
    }

    @Transactional(readOnly = true)
    List<OperatorAccount> listAllAccounts() {
        return operatorAccountRepository.findAll();
    }

    @Transactional(readOnly = true)
    OperatorAccount getAccount(UUID accountId) {
        return findAccount(accountId);
    }

    @Transactional(readOnly = true)
    List<StudentOperatorController.MemberResponse> listMembersWithUsers(UUID accountId) {
        return operatorMembershipRepository.findAllByOperatorAccountId(accountId).stream()
                .map(m -> {
                    var user = userQueryService.findById(m.getUserId()).orElse(null);
                    return StudentOperatorController.MemberResponse.from(m, user);
                })
                .toList();
    }

    @Transactional(readOnly = true)
    List<OperatorMembership> listMembers(UUID accountId) {
        findAccount(accountId);
        return operatorMembershipRepository.findAllByOperatorAccountId(accountId);
    }

    // ── API key operations (unchanged, OWNER only) ────────────────────────────

    @Transactional
    OperatorApiKeyCreationResult createApiKey(UUID accountId, UUID userId, String appName, String description) {
        requireOwner(accountId, userId);
        var result = apiKeyManagementPort.createApiKey(appName, description, userId);
        var account = findAccount(accountId);
        account.setOwnedApiKeyId(result.apiKey().getId());
        operatorAccountRepository.save(account);
        return new OperatorApiKeyCreationResult(result.rawKey(), result.apiKey());
    }

    @Transactional(readOnly = true)
    OperatorApiKeyView getApiKey(UUID accountId, UUID userId) {
        requireMember(accountId, userId);
        var account = findAccount(accountId);
        if (account.getOwnedApiKeyId() == null) return null;
        return apiKeyManagementPort.listAll().stream()
                .filter(k -> k.getId().equals(account.getOwnedApiKeyId()))
                .findFirst().map(OperatorApiKeyView::from).orElse(null);
    }

    @Transactional
    OperatorApiKeyRotationResult rotateApiKey(UUID accountId, UUID userId) {
        requireOwner(accountId, userId);
        var account = findAccount(accountId);
        if (account.getOwnedApiKeyId() == null)
            throw new OperatorAccountNotFoundException("No API key linked to this operator account");
        var result = apiKeyManagementPort.rotateApiKey(account.getOwnedApiKeyId(), userId);
        return new OperatorApiKeyRotationResult(result.rawKey(), result.applicationName(), result.rotatedAt());
    }

    @Transactional
    void deleteApiKey(UUID accountId, UUID userId) {
        requireOwner(accountId, userId);
        var account = findAccount(accountId);
        if (account.getOwnedApiKeyId() == null)
            throw new OperatorAccountNotFoundException("No API key linked to this operator account");
        apiKeyManagementPort.revokeApiKey(account.getOwnedApiKeyId(), userId);
        account.setOwnedApiKeyId(null);
        operatorAccountRepository.save(account);
    }

    /** Public membership check — used by other controllers to gate access. */
    @Transactional(readOnly = true)
    void requireActiveMember(UUID accountId, UUID userId) {
        requireMember(accountId, userId);
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private OperatorAccount findAccount(UUID accountId) {
        return operatorAccountRepository.findById(accountId)
                .orElseThrow(() -> new OperatorAccountNotFoundException("Operator account not found: " + accountId));
    }

    private void requireOwner(UUID accountId, UUID userId) {
        var m = operatorMembershipRepository.findByOperatorAccountIdAndUserId(accountId, userId)
                .orElseThrow(() -> new OperatorOwnershipRequiredException("You are not a member of this operator account"));
        if (m.getStatus() != OperatorMemberStatus.ACTIVE)
            throw new OperatorOwnershipRequiredException("Membership is not active");
        if (m.getMemberRole() != OperatorMemberRole.OWNER)
            throw new OperatorOwnershipRequiredException("Only the OWNER can perform this action");
    }

    private void requireMember(UUID accountId, UUID userId) {
        var m = operatorMembershipRepository.findByOperatorAccountIdAndUserId(accountId, userId)
                .orElseThrow(() -> new OperatorOwnershipRequiredException("You are not a member of this operator account"));
        if (m.getStatus() != OperatorMemberStatus.ACTIVE)
            throw new OperatorOwnershipRequiredException("Membership is not active");
    }

    private void afterCommit(Runnable action) {
        if (TransactionSynchronizationManager.isActualTransactionActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override public void afterCommit() { action.run(); }
            });
        } else {
            action.run();
        }
    }
}
