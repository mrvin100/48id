package io.k48.fortyeightid.operator.internal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import io.k48.fortyeightid.admin.TrafficQueryPort;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AdminOperatorAccountControllerTest {

    @Mock private OperatorAccountService operatorAccountService;
    @Mock private TrafficQueryPort trafficQueryPort;
    @InjectMocks private AdminOperatorAccountController controller;

    @Test
    void listAccounts_returnsAllAccounts() {
        var account = buildAccount(UUID.randomUUID(), "K48 Ops");
        when(operatorAccountService.listAllAccounts()).thenReturn(List.of(account));

        var response = controller.listAccounts();

        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(response.getBody()).hasSize(1);
        assertThat(response.getBody().get(0).name()).isEqualTo("K48 Ops");
    }

    @Test
    void getAccount_returnsAccount() {
        var accountId = UUID.randomUUID();
        var account = buildAccount(accountId, "K48 Ops");
        when(operatorAccountService.getAccount(accountId)).thenReturn(account);

        var response = controller.getAccount(accountId);

        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(response.getBody().name()).isEqualTo("K48 Ops");
    }

    @Test
    void listMembers_returnsMembersForAccount() {
        var accountId = UUID.randomUUID();
        var member = buildMembership(UUID.randomUUID(), OperatorMemberRole.OWNER, OperatorMemberStatus.ACTIVE);
        when(operatorAccountService.listMembers(accountId)).thenReturn(List.of(member));

        var response = controller.listMembers(accountId);

        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(response.getBody()).hasSize(1);
        assertThat(response.getBody().get(0).memberRole()).isEqualTo("OWNER");
    }

    private OperatorAccount buildAccount(UUID id, String name) {
        return OperatorAccount.builder().id(id).name(name).createdAt(Instant.now()).build();
    }

    private OperatorMembership buildMembership(UUID userId, OperatorMemberRole role, OperatorMemberStatus status) {
        return OperatorMembership.builder().id(UUID.randomUUID()).operatorAccountId(UUID.randomUUID()).userId(userId)
                .memberRole(role).status(status).createdAt(Instant.now()).build();
    }
}
