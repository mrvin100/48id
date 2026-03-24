package io.k48.fortyeightid.operator.internal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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
    @InjectMocks private AdminOperatorAccountController controller;

    @Test
    void listAccounts_returnsAllAccounts() {
        var account = buildAccount(UUID.randomUUID(), "K48 Ops");
        when(operatorAccountService.listAccounts()).thenReturn(List.of(account));

        var response = controller.listAccounts();

        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(response.getBody()).hasSize(1);
        assertThat(response.getBody().get(0).name()).isEqualTo("K48 Ops");
    }

    @Test
    void createAccount_returns201WithBody() {
        var adminId = UUID.randomUUID();
        var request = new AdminOperatorAccountController.CreateAccountRequest("K48 Ops", "desc");
        var account = buildAccount(UUID.randomUUID(), "K48 Ops");

        when(operatorAccountService.createAccount(eq("K48 Ops"), eq("desc"), any(UUID.class))).thenReturn(account);

        var response = controller.createAccount(request, adminId.toString());

        assertThat(response.getStatusCode().value()).isEqualTo(201);
        assertThat(response.getBody().name()).isEqualTo("K48 Ops");
    }

    @Test
    void deleteAccount_returns204() {
        var accountId = UUID.randomUUID();
        var adminId = UUID.randomUUID();

        var response = controller.deleteAccount(accountId, adminId.toString());

        assertThat(response.getStatusCode().value()).isEqualTo(204);
        verify(operatorAccountService).deleteAccount(accountId, adminId);
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

    @Test
    void inviteMember_returns201() {
        var accountId = UUID.randomUUID();
        var adminId = UUID.randomUUID();
        var request = new AdminOperatorAccountController.InviteRequest(UUID.randomUUID(), "OWNER");

        var response = controller.inviteMember(accountId, request, adminId.toString());

        assertThat(response.getStatusCode().value()).isEqualTo(201);
        verify(operatorAccountService).inviteMember(eq(accountId), eq(request.userId()), eq("OWNER"), eq(adminId));
    }

    @Test
    void removeMember_returns204() {
        var accountId = UUID.randomUUID();
        var userId = UUID.randomUUID();
        var adminId = UUID.randomUUID();

        var response = controller.removeMember(accountId, userId, adminId.toString());

        assertThat(response.getStatusCode().value()).isEqualTo(204);
        verify(operatorAccountService).removeMember(accountId, userId, adminId);
    }

    private OperatorAccount buildAccount(UUID id, String name) {
        return OperatorAccount.builder().id(id).name(name).createdAt(Instant.now()).build();
    }

    private OperatorMembership buildMembership(UUID userId, OperatorMemberRole role, OperatorMemberStatus status) {
        return OperatorMembership.builder().id(UUID.randomUUID()).userId(userId)
                .memberRole(role).status(status).createdAt(Instant.now()).build();
    }
}
