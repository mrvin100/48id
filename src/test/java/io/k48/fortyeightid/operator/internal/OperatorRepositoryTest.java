package io.k48.fortyeightid.operator.internal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.k48.fortyeightid.TestcontainersConfiguration;
import io.k48.fortyeightid.identity.User;
import io.k48.fortyeightid.identity.UserStatus;
import io.k48.fortyeightid.identity.internal.UserRepository;
import io.k48.fortyeightid.operator.internal.OperatorMembership.MemberRole;
import io.k48.fortyeightid.operator.internal.OperatorMembership.MembershipStatus;
import io.k48.fortyeightid.shared.config.JpaAuditingConfig;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({TestcontainersConfiguration.class, JpaAuditingConfig.class})
class OperatorRepositoryTest {

    @Autowired
    private OperatorAccountRepository accountRepository;

    @Autowired
    private OperatorMembershipRepository membershipRepository;

    @Autowired
    private UserRepository userRepository;

    private OperatorAccount savedAccount;
    private UUID userId1;
    private UUID userId2;

    @BeforeEach
    void setUp() {
        membershipRepository.deleteAll();
        accountRepository.deleteAll();
        userRepository.deleteAll();

        userId1 = userRepository.save(User.builder()
            .matricule("K48-TEST-001").email("user1@k48.io").name("User One")
            .passwordHash("hash").status(UserStatus.ACTIVE).build()).getId();

        userId2 = userRepository.save(User.builder()
            .matricule("K48-TEST-002").email("user2@k48.io").name("User Two")
            .passwordHash("hash").status(UserStatus.ACTIVE).build()).getId();

        savedAccount = accountRepository.save(OperatorAccount.builder()
            .name("48Hub Team")
            .description("Main hub application")
            .build());
    }

    // --- OperatorAccount ---

    @Test
    void save_shouldPersistAccountWithGeneratedUuidAndCreatedAt() {
        assertThat(savedAccount.getId()).isNotNull();
        assertThat(savedAccount.getCreatedAt()).isNotNull();
        assertThat(savedAccount.getName()).isEqualTo("48Hub Team");
    }

    @Test
    void save_shouldThrow_whenDuplicateName() {
        assertThatThrownBy(() ->
            accountRepository.saveAndFlush(OperatorAccount.builder().name("48Hub Team").build())
        ).isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void findByName_shouldReturnAccount() {
        assertThat(accountRepository.findByName("48Hub Team")).isPresent();
    }

    // --- OperatorMembership ---

    @Test
    void save_shouldPersistMembershipWithPendingAndCollaboratorDefaults() {
        var membership = membershipRepository.save(OperatorMembership.builder()
            .accountId(savedAccount.getId())
            .userId(userId1)
            .build());

        assertThat(membership.getId()).isNotNull();
        assertThat(membership.getStatus()).isEqualTo(MembershipStatus.PENDING);
        assertThat(membership.getMemberRole()).isEqualTo(MemberRole.COLLABORATOR);
        assertThat(membership.getInvitedAt()).isNotNull();
        assertThat(membership.getAcceptedAt()).isNull();
    }

    @Test
    void save_shouldThrow_whenUserAddedTwiceToSameAccount() {
        membershipRepository.saveAndFlush(OperatorMembership.builder()
            .accountId(savedAccount.getId()).userId(userId1).build());

        assertThatThrownBy(() -> membershipRepository.saveAndFlush(OperatorMembership.builder()
            .accountId(savedAccount.getId()).userId(userId1).build()))
            .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void save_shouldAllow_whenUserMemberOfMultipleAccounts() {
        var account2 = accountRepository.save(OperatorAccount.builder().name("LP48 Team").build());

        membershipRepository.save(OperatorMembership.builder()
            .accountId(savedAccount.getId()).userId(userId1).build());
        membershipRepository.save(OperatorMembership.builder()
            .accountId(account2.getId()).userId(userId1).build());

        assertThat(membershipRepository.findByUserId(userId1)).hasSize(2);
    }

    @Test
    void findByAccountId_shouldReturnAllMembers() {
        membershipRepository.save(OperatorMembership.builder().accountId(savedAccount.getId()).userId(userId1).build());
        membershipRepository.save(OperatorMembership.builder().accountId(savedAccount.getId()).userId(userId2).build());

        assertThat(membershipRepository.findByAccountId(savedAccount.getId())).hasSize(2);
    }

    @Test
    void findByUserId_shouldReturnAllAccountsForUser() {
        var account2 = accountRepository.save(OperatorAccount.builder().name("LP48 Team").build());
        membershipRepository.save(OperatorMembership.builder().accountId(savedAccount.getId()).userId(userId1).build());
        membershipRepository.save(OperatorMembership.builder().accountId(account2.getId()).userId(userId1).build());

        assertThat(membershipRepository.findByUserId(userId1)).hasSize(2);
    }

    @Test
    void findByAccountIdAndMemberRole_shouldReturnOnlyOwners() {
        membershipRepository.save(OperatorMembership.builder()
            .accountId(savedAccount.getId()).userId(userId1).memberRole(MemberRole.OWNER).build());
        membershipRepository.save(OperatorMembership.builder()
            .accountId(savedAccount.getId()).userId(userId2).memberRole(MemberRole.COLLABORATOR).build());

        var owners = membershipRepository.findByAccountIdAndMemberRole(savedAccount.getId(), MemberRole.OWNER);
        assertThat(owners).hasSize(1);
        assertThat(owners.get(0).getUserId()).isEqualTo(userId1);
    }

    @Test
    void existsByAccountIdAndUserId_shouldReturnTrueWhenExists() {
        membershipRepository.save(OperatorMembership.builder()
            .accountId(savedAccount.getId()).userId(userId1).build());

        assertThat(membershipRepository.existsByAccountIdAndUserId(savedAccount.getId(), userId1)).isTrue();
        assertThat(membershipRepository.existsByAccountIdAndUserId(savedAccount.getId(), userId2)).isFalse();
    }
}
