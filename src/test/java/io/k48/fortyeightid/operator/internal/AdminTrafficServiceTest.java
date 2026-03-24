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
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

@ExtendWith(MockitoExtension.class)
class AdminTrafficServiceTest {

    @Mock private OperatorAccountRepository operatorAccountRepository;
    @Mock private OperatorMembershipRepository operatorMembershipRepository;
    @Mock private AuditLogRepository auditLogRepository;
    @Mock private UserQueryService userQueryService;
    @InjectMocks private AdminTrafficService adminTrafficService;

    @Test
    void getAggregatedTraffic_noAccounts_returnsEmptyList() {
        when(operatorAccountRepository.findAll()).thenReturn(List.of());

        var result = adminTrafficService.getAggregatedTraffic();

        assertThat(result.accounts()).isEmpty();
        assertThat(result.generatedAt()).isNotNull();
    }

    @Test
    void getAggregatedTraffic_accountWithNoApiKey_returnsZeroCounts() {
        var account = OperatorAccount.builder()
                .id(UUID.randomUUID()).name("Test").ownedApiKeyId(null).build();
        when(operatorAccountRepository.findAll()).thenReturn(List.of(account));
        when(operatorMembershipRepository.findAllByOperatorAccountId(account.getId())).thenReturn(List.of());

        var result = adminTrafficService.getAggregatedTraffic();

        assertThat(result.accounts()).hasSize(1);
        var traffic = result.accounts().get(0);
        assertThat(traffic.apiKeyTraffic().totalCalls()).isZero();
        assertThat(traffic.memberActivity().totalActions()).isZero();
    }

    @Test
    void getAggregatedTraffic_accountWithApiKeyCalls_returnsCounts() {
        var keyId = UUID.randomUUID();
        var account = OperatorAccount.builder()
                .id(UUID.randomUUID()).name("Hub").ownedApiKeyId(keyId).build();
        when(operatorAccountRepository.findAll()).thenReturn(List.of(account));
        when(operatorMembershipRepository.findAllByOperatorAccountId(account.getId())).thenReturn(List.of());

        var call1 = AuditLog.builder().action("API_KEY_USED").createdAt(Instant.now()).build();
        var call2 = AuditLog.builder().action("API_KEY_USED").createdAt(Instant.now().minusSeconds(3600)).build();
        when(auditLogRepository.findApiKeyUsageByKeyId(keyId.toString())).thenReturn(List.of(call1, call2));

        var result = adminTrafficService.getAggregatedTraffic();

        assertThat(result.accounts().get(0).apiKeyTraffic().totalCalls()).isEqualTo(2);
        assertThat(result.accounts().get(0).apiKeyTraffic().last24h()).isEqualTo(2);
    }

    @Test
    void getAggregatedTraffic_noUserLevelDataExposed() {
        var account = OperatorAccount.builder()
                .id(UUID.randomUUID()).name("Hub").ownedApiKeyId(null).build();
        when(operatorAccountRepository.findAll()).thenReturn(List.of(account));
        when(operatorMembershipRepository.findAllByOperatorAccountId(account.getId())).thenReturn(List.of());

        var result = adminTrafficService.getAggregatedTraffic();

        var traffic = result.accounts().get(0);
        // Use record components — stable, not brittle reflection
        var componentNames = java.util.Arrays.stream(traffic.getClass().getRecordComponents())
                .map(c -> c.getName()).toList();
        assertThat(componentNames).doesNotContain("userId", "matricule", "email");
    }

    @Test
    void getAccountTrafficDetail_unknownAccount_throwsOperatorAccountNotFoundException() {
        var accountId = UUID.randomUUID();
        when(operatorAccountRepository.findById(accountId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> adminTrafficService.getAccountTrafficDetail(accountId, PageRequest.of(0, 50)))
                .isInstanceOf(OperatorAccountNotFoundException.class);
    }

    @Test
    void getAccountTrafficDetail_paginatesApiKeyCalls() {
        var accountId = UUID.randomUUID();
        var keyId = UUID.randomUUID();
        var account = OperatorAccount.builder().id(accountId).name("Hub").ownedApiKeyId(keyId).build();
        when(operatorAccountRepository.findById(accountId)).thenReturn(Optional.of(account));
        when(operatorMembershipRepository.findAllByOperatorAccountId(accountId)).thenReturn(List.of());

        var call = AuditLog.builder().action("API_KEY_USED")
                .createdAt(Instant.now()).details("{}").ipAddress("1.2.3.4").build();
        var pageable = PageRequest.of(0, 1);
        when(auditLogRepository.findApiKeyUsageByKeyIdPaged(keyId.toString(), pageable))
                .thenReturn(new PageImpl<>(List.of(call), pageable, 5));

        var result = adminTrafficService.getAccountTrafficDetail(accountId, pageable);

        assertThat(result.apiKeyCalls().getContent()).hasSize(1);
        assertThat(result.apiKeyCalls().getTotalElements()).isEqualTo(5);
        assertThat(result.apiKeyCalls().getTotalPages()).isEqualTo(5);
    }
}
