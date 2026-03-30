package io.k48.fortyeightid.operator.internal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import io.k48.fortyeightid.audit.AuditLog;
import io.k48.fortyeightid.audit.AuditLogRepository;
import io.k48.fortyeightid.identity.UserQueryService;
import io.k48.fortyeightid.shared.exception.OperatorAccountNotFoundException;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class OperatorTrafficServiceTest {

    @Mock private OperatorAccountRepository operatorAccountRepository;
    @Mock private OperatorMembershipRepository operatorMembershipRepository;
    @Mock private AuditLogRepository auditLogRepository;
    @Mock private UserQueryService userQueryService;
    @InjectMocks private OperatorTrafficService operatorTrafficService;

    @Test
    void getTraffic_accountNotFound_throws() {
        var accountId = UUID.randomUUID();
        when(operatorAccountRepository.findById(accountId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> operatorTrafficService.getTrafficForAccount(accountId))
                .isInstanceOf(OperatorAccountNotFoundException.class);
    }

    @Test
    void getTraffic_withApiKey_returnsApiKeyCalls() {
        var accountId = UUID.randomUUID();
        var userId = UUID.randomUUID();
        var keyId = UUID.randomUUID();

        var account = OperatorAccount.builder()
                .id(accountId).name("Hub").ownedApiKeyId(keyId).build();
        when(operatorAccountRepository.findById(accountId)).thenReturn(Optional.of(account));

        var call = AuditLog.builder().action("API_KEY_USED")
                .createdAt(Instant.now()).details("{\"endpoint\":\"/api/v1/users\",\"method\":\"GET\"}").build();
        when(auditLogRepository.findApiKeyUsageByKeyId(keyId.toString())).thenReturn(List.of(call));

        var membership = OperatorMembership.builder()
                .operatorAccountId(accountId).userId(userId)
                .memberRole(OperatorMemberRole.OWNER).status(OperatorMemberStatus.ACTIVE).build();
        when(operatorMembershipRepository.findAllByOperatorAccountId(accountId)).thenReturn(List.of(membership));
        when(auditLogRepository.findOperatorActionsByUserIds(List.of(userId))).thenReturn(List.of());
        when(userQueryService.findById(userId)).thenReturn(Optional.empty());

        var result = operatorTrafficService.getTrafficForAccount(accountId);

        assertThat(result.apiKeyCalls()).hasSize(1);
        assertThat(result.apiKeyCalls().get(0).totalInWindow()).isEqualTo(1);
        assertThat(result.generatedAt()).isNotNull();
    }

    @Test
    void getTraffic_noApiKey_returnsEmptyApiKeyCalls() {
        var accountId = UUID.randomUUID();
        var userId = UUID.randomUUID();

        var account = OperatorAccount.builder()
                .id(accountId).name("Hub").ownedApiKeyId(null).build();
        when(operatorAccountRepository.findById(accountId)).thenReturn(Optional.of(account));

        var membership = OperatorMembership.builder()
                .operatorAccountId(accountId).userId(userId)
                .memberRole(OperatorMemberRole.COLLABORATOR).status(OperatorMemberStatus.ACTIVE).build();
        when(operatorMembershipRepository.findAllByOperatorAccountId(accountId)).thenReturn(List.of(membership));
        when(auditLogRepository.findOperatorActionsByUserIds(List.of(userId))).thenReturn(List.of());
        when(userQueryService.findById(userId)).thenReturn(Optional.empty());

        var result = operatorTrafficService.getTrafficForAccount(accountId);

        assertThat(result.apiKeyCalls()).isEmpty();
    }
}
