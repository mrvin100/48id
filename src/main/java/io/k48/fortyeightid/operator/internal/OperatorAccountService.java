package io.k48.fortyeightid.operator.internal;

import io.k48.fortyeightid.audit.AuditService;
import io.k48.fortyeightid.auth.ApiKeyManagementPort;
import io.k48.fortyeightid.auth.EmailPort;
import io.k48.fortyeightid.identity.UserQueryService;
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

@Slf4j
@Service
@RequiredArgsConstructor
class OperatorAccountService {

    private static final long INVITE_TOKEN_TTL_SECONDS = 86400;

    private final OperatorAccountRepository operatorAccountRepository;
    private final OperatorMembershipRepository operatorMembershipRepository;
    private final OperatorInviteTokenService operatorInviteTokenService;
    private final ApiKeyManagementPort apiKeyManagementPort;
    private final UserQueryService userQueryService;
    private final AuditService auditService;
    private final EmailPort emailPort;

    // ── Admin operations ──────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    List<OperatorAccount> listAccounts() {
        return operatorAccountRepository.findAll();
    }

    @Transactional
    OperatorAccount createAccount(String name, String description, UUID adminId) {
        var saved = operatorAccountRepository.save(OperatorAccount.builder().name(name).description(description).build());
        auditService.log(adminId, "OPERATOR_ACCOUNT_CREATED", Map.of("accountId", saved.getId().toString(), "name", name));
        return saved;
    }

    @Transactional(readOnly = true)
    OperatorAccount getAccount(UUID accountId) {
        return findAccount(accountId);
    }

    @Transactional
    void deleteAccount(UUID accountId, UUID adminId) {
        operatorAccountRepository.delete(findAccount(accountId));
        auditService.log(adminId, "OPERATOR_ACCOUNT_DELETED", Map.of("accountId", accountId.toString()));
    }

    @Transactional(readOnly = true)
    List<OperatorMembership> listMembers(UUID accountId) {
        findAccount(accountId);
        return operatorMembershipRepository.findAllByOperatorAccountId(accountId);
    }

    @Transactional
    void inviteMember(UUID accountId, UUID targetUserId, String role, UUID adminId) {
        findAccount(accountId);
        var user = userQueryService.findById(targetUserId)
                .orElseThrow(() -> new UserNotFoundException("User not found: " + targetUserId));

        operatorMembershipRepository.save(OperatorMembership.builder()
                .operatorAccountId(accountId)
                .userId(targetUserId)
                .memberRole(OperatorMemberRole.valueOf(role))
                .status(OperatorMemberStatus.PENDING)
                .build());

        var rawToken = operatorInviteTokenService.createInviteToken(targetUserId, INVITE_TOKEN_TTL_SECONDS);
        emailPort.sendOperatorInviteEmail(user.getEmail(), user.getName(), rawToken);
        auditService.log(adminId, "OPERATOR_MEMBER_INVITED",
                Map.of("accountId", accountId.toString(), "targetUser", targetUserId.toString(), "role", role));
        log.info("Operator invite sent to user {} for account {}", targetUserId, accountId);
    }

    @Transactional
    void removeMember(UUID accountId, UUID targetUserId, UUID adminId) {
        findAccount(accountId);
        operatorMembershipRepository.deleteByOperatorAccountIdAndUserId(accountId, targetUserId);
        auditService.log(adminId, "OPERATOR_MEMBER_REMOVED",
                Map.of("accountId", accountId.toString(), "targetUser", targetUserId.toString()));
    }

    // ── Operator API key operations ───────────────────────────────────────────

    @Transactional
    OperatorApiKeyCreationResult createApiKey(UUID operatorAccountId, UUID userId, String appName, String description) {
        requireOwner(operatorAccountId, userId);
        var result = apiKeyManagementPort.createApiKey(appName, description, userId);
        var account = findAccount(operatorAccountId);
        account.setOwnedApiKeyId(result.apiKey().getId());
        operatorAccountRepository.save(account);
        return new OperatorApiKeyCreationResult(result.rawKey(), result.apiKey());
    }

    @Transactional(readOnly = true)
    OperatorApiKeyView getApiKey(UUID operatorAccountId, UUID userId) {
        requireMember(operatorAccountId, userId);
        var account = findAccount(operatorAccountId);
        if (account.getOwnedApiKeyId() == null) return null;
        return apiKeyManagementPort.listAll().stream()
                .filter(k -> k.getId().equals(account.getOwnedApiKeyId()))
                .findFirst().map(OperatorApiKeyView::from).orElse(null);
    }

    @Transactional
    OperatorApiKeyRotationResult rotateApiKey(UUID operatorAccountId, UUID userId) {
        requireOwner(operatorAccountId, userId);
        var account = findAccount(operatorAccountId);
        if (account.getOwnedApiKeyId() == null)
            throw new OperatorAccountNotFoundException("No API key linked to this operator account");
        var result = apiKeyManagementPort.rotateApiKey(account.getOwnedApiKeyId(), userId);
        return new OperatorApiKeyRotationResult(result.rawKey(), result.applicationName(), result.rotatedAt());
    }

    @Transactional
    void deleteApiKey(UUID operatorAccountId, UUID userId) {
        requireOwner(operatorAccountId, userId);
        var account = findAccount(operatorAccountId);
        if (account.getOwnedApiKeyId() == null)
            throw new OperatorAccountNotFoundException("No API key linked to this operator account");
        apiKeyManagementPort.revokeApiKey(account.getOwnedApiKeyId(), userId);
        account.setOwnedApiKeyId(null);
        operatorAccountRepository.save(account);
    }

    // ── Invite acceptance ─────────────────────────────────────────────────────

    @Transactional
    void acceptInvite(UUID userId) {
        var membership = operatorMembershipRepository.findByUserIdAndStatus(userId, OperatorMemberStatus.PENDING)
                .orElseThrow(() -> new OperatorAccountNotFoundException("No pending invite found for user: " + userId));
        membership.setStatus(OperatorMemberStatus.ACTIVE);
        operatorMembershipRepository.save(membership);
        log.info("Operator invite accepted by user {}", userId);
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private OperatorAccount findAccount(UUID accountId) {
        return operatorAccountRepository.findById(accountId)
                .orElseThrow(() -> new OperatorAccountNotFoundException("Operator account not found: " + accountId));
    }

    private void requireOwner(UUID accountId, UUID userId) {
        var m = operatorMembershipRepository.findByOperatorAccountIdAndUserId(accountId, userId)
                .orElseThrow(() -> new OperatorOwnershipRequiredException("You are not a member of this operator account"));
        if (m.getMemberRole() != OperatorMemberRole.OWNER)
            throw new OperatorOwnershipRequiredException("Only the OWNER can perform this action");
    }

    private void requireMember(UUID accountId, UUID userId) {
        operatorMembershipRepository.findByOperatorAccountIdAndUserId(accountId, userId)
                .orElseThrow(() -> new OperatorOwnershipRequiredException("You are not a member of this operator account"));
    }
}
