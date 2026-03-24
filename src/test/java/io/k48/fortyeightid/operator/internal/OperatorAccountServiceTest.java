package io.k48.fortyeightid.operator.internal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.k48.fortyeightid.audit.AuditService;
import io.k48.fortyeightid.auth.ApiKey;
import io.k48.fortyeightid.auth.ApiKeyManagementPort;
import io.k48.fortyeightid.auth.ApiKeyManagementPort.ApiKeyCreationResult;
import io.k48.fortyeightid.auth.EmailPort;
import io.k48.fortyeightid.identity.User;
import io.k48.fortyeightid.identity.UserQueryService;
import io.k48.fortyeightid.shared.exception.OperatorAccountNotFoundException;
import io.k48.fortyeightid.shared.exception.OperatorOwnershipRequiredException;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class OperatorAccountServiceTest {

    @Mock private OperatorAccountRepository operatorAccountRepository;
    @Mock private OperatorMembershipRepository operatorMembershipRepository;
    @Mock private OperatorInviteTokenService operatorInviteTokenService;
    @Mock private ApiKeyManagementPort apiKeyManagementPort;
    @Mock private UserQueryService userQueryService;
    @Mock private AuditService auditService;
    @Mock private EmailPort emailPort;

    @InjectMocks private OperatorAccountService service;

    // ── Account creation ──────────────────────────────────────────────────────

    @Test
    void createAccount_savesAndReturnsAccount() {
        var adminId = UUID.randomUUID();
        var saved = buildAccount(UUID.randomUUID(), "K48 Ops");
        when(operatorAccountRepository.save(any())).thenReturn(saved);

        var result = service.createAccount("K48 Ops", null, adminId);

        assertThat(result.getName()).isEqualTo("K48 Ops");
        assertThat(result.getId()).isEqualTo(saved.getId());
        verify(auditService).log(eq(adminId), eq("OPERATOR_ACCOUNT_CREATED"), any());
    }

    // ── Invite — email sent after commit ──────────────────────────────────────

    @Test
    void inviteMember_savesMembershipAndRegistersAfterCommitEmail() {
        var adminId = UUID.randomUUID();
        var accountId = UUID.randomUUID();
        var userId = UUID.randomUUID();
        var account = buildAccount(accountId, "K48 Ops");
        var user = buildUser(userId);

        when(operatorAccountRepository.findById(accountId)).thenReturn(Optional.of(account));
        when(userQueryService.findById(userId)).thenReturn(Optional.of(user));
        when(operatorInviteTokenService.createInviteToken(eq(userId), anyLong())).thenReturn("raw-token");

        service.inviteMember(accountId, userId, "OWNER", adminId);

        verify(operatorMembershipRepository).save(any(OperatorMembership.class));
        // No active transaction in unit test — email is called directly (fallback path)
        verify(emailPort).sendOperatorInviteEmail(eq(user.getEmail()), eq(user.getName()), eq("raw-token"));
        verify(auditService).log(eq(adminId), eq("OPERATOR_MEMBER_INVITED"), any());
    }

    // ── Accept invite — exact lookup by accountId ─────────────────────────────

    @Test
    void acceptInvite_transitionsPendingToActive() {
        var userId = UUID.randomUUID();
        var accountId = UUID.randomUUID();
        var membership = buildMembership(accountId, userId, OperatorMemberRole.OWNER, OperatorMemberStatus.PENDING);

        when(operatorMembershipRepository.findByOperatorAccountIdAndUserIdAndStatus(accountId, userId, OperatorMemberStatus.PENDING))
                .thenReturn(Optional.of(membership));

        service.acceptInvite(userId, accountId);

        assertThat(membership.getStatus()).isEqualTo(OperatorMemberStatus.ACTIVE);
        verify(operatorMembershipRepository).save(membership);
    }

    @Test
    void acceptInvite_throwsWhenNoPendingInvite() {
        var userId = UUID.randomUUID();
        var accountId = UUID.randomUUID();
        when(operatorMembershipRepository.findByOperatorAccountIdAndUserIdAndStatus(accountId, userId, OperatorMemberStatus.PENDING))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.acceptInvite(userId, accountId))
                .isInstanceOf(OperatorAccountNotFoundException.class);
    }

    // ── PENDING member cannot access API key operations ───────────────────────

    @Test
    void createApiKey_pendingMemberCannotCreate() {
        var accountId = UUID.randomUUID();
        var userId = UUID.randomUUID();
        var membership = buildMembership(accountId, userId, OperatorMemberRole.OWNER, OperatorMemberStatus.PENDING);

        when(operatorMembershipRepository.findByOperatorAccountIdAndUserId(accountId, userId))
                .thenReturn(Optional.of(membership));

        assertThatThrownBy(() -> service.createApiKey(accountId, userId, "MyApp", "desc"))
                .isInstanceOf(OperatorOwnershipRequiredException.class)
                .hasMessageContaining("not active");
    }

    // ── API key — OWNER (ACTIVE) can create ───────────────────────────────────

    @Test
    void createApiKey_activeOwnerCanCreate() {
        var accountId = UUID.randomUUID();
        var userId = UUID.randomUUID();
        var account = buildAccount(accountId, "K48 Ops");
        var membership = buildMembership(accountId, userId, OperatorMemberRole.OWNER, OperatorMemberStatus.ACTIVE);
        var apiKey = buildApiKey(UUID.randomUUID());
        var creationResult = new ApiKeyCreationResult("raw-key", apiKey);

        when(operatorMembershipRepository.findByOperatorAccountIdAndUserId(accountId, userId))
                .thenReturn(Optional.of(membership));
        when(operatorAccountRepository.findById(accountId)).thenReturn(Optional.of(account));
        when(apiKeyManagementPort.createApiKey(anyString(), anyString(), eq(userId))).thenReturn(creationResult);
        when(operatorAccountRepository.save(any())).thenReturn(account);

        var result = service.createApiKey(accountId, userId, "MyApp", "desc");

        assertThat(result.rawKey()).isEqualTo("raw-key");
        assertThat(account.getOwnedApiKeyId()).isEqualTo(apiKey.getId());
    }

    // ── COLLABORATOR cannot modify ────────────────────────────────────────────

    @Test
    void createApiKey_collaboratorCannotCreate() {
        var accountId = UUID.randomUUID();
        var userId = UUID.randomUUID();
        var membership = buildMembership(accountId, userId, OperatorMemberRole.COLLABORATOR, OperatorMemberStatus.ACTIVE);

        when(operatorMembershipRepository.findByOperatorAccountIdAndUserId(accountId, userId))
                .thenReturn(Optional.of(membership));

        assertThatThrownBy(() -> service.createApiKey(accountId, userId, "MyApp", "desc"))
                .isInstanceOf(OperatorOwnershipRequiredException.class)
                .hasMessageContaining("OWNER");
    }

    @Test
    void rotateApiKey_collaboratorCannotRotate() {
        var accountId = UUID.randomUUID();
        var userId = UUID.randomUUID();
        var membership = buildMembership(accountId, userId, OperatorMemberRole.COLLABORATOR, OperatorMemberStatus.ACTIVE);

        when(operatorMembershipRepository.findByOperatorAccountIdAndUserId(accountId, userId))
                .thenReturn(Optional.of(membership));

        assertThatThrownBy(() -> service.rotateApiKey(accountId, userId))
                .isInstanceOf(OperatorOwnershipRequiredException.class);
    }

    @Test
    void deleteApiKey_collaboratorCannotDelete() {
        var accountId = UUID.randomUUID();
        var userId = UUID.randomUUID();
        var membership = buildMembership(accountId, userId, OperatorMemberRole.COLLABORATOR, OperatorMemberStatus.ACTIVE);

        when(operatorMembershipRepository.findByOperatorAccountIdAndUserId(accountId, userId))
                .thenReturn(Optional.of(membership));

        assertThatThrownBy(() -> service.deleteApiKey(accountId, userId))
                .isInstanceOf(OperatorOwnershipRequiredException.class);
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private OperatorAccount buildAccount(UUID id, String name) {
        return OperatorAccount.builder().id(id).name(name).createdAt(Instant.now()).build();
    }

    private OperatorMembership buildMembership(UUID accountId, UUID userId, OperatorMemberRole role, OperatorMemberStatus status) {
        return OperatorMembership.builder()
                .id(UUID.randomUUID())
                .operatorAccountId(accountId)
                .userId(userId)
                .memberRole(role)
                .status(status)
                .createdAt(Instant.now())
                .build();
    }

    private User buildUser(UUID id) {
        return User.builder().id(id).email("operator@k48.io").name("Operator User")
                .matricule("K48-2024-001").passwordHash("hash").build();
    }

    private ApiKey buildApiKey(UUID id) {
        return ApiKey.builder().id(id).appName("MyApp").keyHash("hash").createdAt(Instant.now()).build();
    }
}
