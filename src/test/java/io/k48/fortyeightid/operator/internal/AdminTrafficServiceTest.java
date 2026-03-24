package io.k48.fortyeightid.operator.internal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.k48.fortyeightid.audit.AuditLog;
import io.k48.fortyeightid.audit.AuditLogRepository;
import io.k48.fortyeightid.identity.UserQueryService;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.mockito.Spy;

@ExtendWith(MockitoExtension.class)
class AdminTrafficServiceTest {

    @Mock private OperatorAccountRepository operatorAccountRepository;
    @Mock private OperatorMembershipRepository operatorMembershipRepository;
    @Mock private AuditLogRepository auditLogRepository;
    @Mock private UserQueryService userQueryService;
    @Spy  private ObjectMapper objectMapper;
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

        // Verify response contains NO user-level fields (only aggregated counts)
        var traffic = result.accounts().get(0);
        assertThat(traffic.apiKeyTraffic()).isNotNull();
        assertThat(traffic.memberActivity()).isNotNull();
        // AccountTraffic has no userId, matricule, or individual action fields
        assertThat(traffic.getClass().getDeclaredFields())
                .extracting(f -> f.getName())
                .doesNotContain("userId", "matricule", "email");
    }
}
