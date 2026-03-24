package io.k48.fortyeightid.operator.internal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.k48.fortyeightid.identity.User;
import io.k48.fortyeightid.identity.UserQueryService;
import io.k48.fortyeightid.identity.UserStatus;
import io.k48.fortyeightid.operator.internal.OperatorMembership.MemberRole;
import io.k48.fortyeightid.operator.internal.OperatorMembership.MembershipStatus;
import io.k48.fortyeightid.operator.OperatorAccountPort.CreateOperatorAccountCommand;
import io.k48.fortyeightid.shared.exception.OperatorAccountNameTakenException;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class OperatorAccountServiceTest {

    @Mock private OperatorAccountRepository accountRepository;
    @Mock private OperatorMembershipRepository membershipRepository;
    @Mock private UserQueryService userQueryService;
    @InjectMocks private OperatorAccountService service;

    private final UUID adminId = UUID.randomUUID();

    private User adminUser() {
        return User.builder()
            .matricule("K48-B1-01").email("admin@k48.io").name("Ulrich")
            .passwordHash("hash").status(UserStatus.ACTIVE).build();
    }

    private OperatorAccount savedAccount(String name) {
        return OperatorAccount.builder()
            .id(UUID.randomUUID()).name(name).createdBy(adminId).build();
    }

    private OperatorMembership savedMembership(UUID accountId) {
        return OperatorMembership.builder()
            .id(UUID.randomUUID()).accountId(accountId).userId(adminId)
            .memberRole(MemberRole.OWNER).status(MembershipStatus.ACTIVE).build();
    }

    @Test
    void createAccount_shouldThrow_whenNameAlreadyTaken() {
        when(accountRepository.existsByName("48Hub Team")).thenReturn(true);

        assertThatThrownBy(() -> service.createAccount(
            new CreateOperatorAccountCommand("48Hub Team", null, adminId)))
            .isInstanceOf(OperatorAccountNameTakenException.class);

        verify(accountRepository, never()).save(any());
    }

    @Test
    void createAccount_shouldPersistAccountAndOwnerMembership() {
        var account = savedAccount("48Hub Team");
        when(accountRepository.existsByName("48Hub Team")).thenReturn(false);
        when(userQueryService.findById(adminId)).thenReturn(Optional.of(adminUser()));
        when(accountRepository.save(any())).thenReturn(account);
        when(membershipRepository.save(any())).thenReturn(savedMembership(account.getId()));

        var result = service.createAccount(
            new CreateOperatorAccountCommand("48Hub Team", "desc", adminId));

        assertThat(result.accountName()).isEqualTo("48Hub Team");
        assertThat(result.ownerId()).isEqualTo(adminId);
        assertThat(result.ownerMatricule()).isEqualTo("K48-B1-01");
        assertThat(result.ownerName()).isEqualTo("Ulrich");
    }

    @Test
    void createAccount_shouldCreateMembershipAsOwnerAndActive() {
        var account = savedAccount("48Hub Team");
        when(accountRepository.existsByName(any())).thenReturn(false);
        when(userQueryService.findById(adminId)).thenReturn(Optional.of(adminUser()));
        when(accountRepository.save(any())).thenReturn(account);
        when(membershipRepository.save(any())).thenReturn(savedMembership(account.getId()));

        service.createAccount(new CreateOperatorAccountCommand("48Hub Team", null, adminId));

        var captor = ArgumentCaptor.forClass(OperatorMembership.class);
        verify(membershipRepository).save(captor.capture());
        assertThat(captor.getValue().getMemberRole()).isEqualTo(MemberRole.OWNER);
        assertThat(captor.getValue().getStatus()).isEqualTo(MembershipStatus.ACTIVE);
        assertThat(captor.getValue().getAcceptedAt()).isNotNull();
    }
}
