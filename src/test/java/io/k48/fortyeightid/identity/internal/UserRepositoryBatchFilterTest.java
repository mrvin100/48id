package io.k48.fortyeightid.identity.internal;

import static org.assertj.core.api.Assertions.assertThat;

import io.k48.fortyeightid.TestcontainersConfiguration;
import io.k48.fortyeightid.identity.User;
import io.k48.fortyeightid.identity.UserStatus;
import io.k48.fortyeightid.shared.config.JpaAuditingConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageRequest;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({TestcontainersConfiguration.class, JpaAuditingConfig.class})
class UserRepositoryBatchFilterTest {

    @Autowired
    private UserRepository userRepository;

    @BeforeEach
    void setUp() {
        userRepository.deleteAll();
        userRepository.save(buildUser("K48-B1-1", "b1a@k48.io", "B1"));
        userRepository.save(buildUser("K48-B1-2", "b1b@k48.io", "B1"));
        userRepository.save(buildUser("K48-B2-1", "b2a@k48.io", "B2"));
    }

    @Test
    void findByBatch_returnsOnlyUsersOfGivenBatch() {
        var page = userRepository.findByBatch("B1", PageRequest.of(0, 10));
        assertThat(page.getContent()).hasSize(2);
        assertThat(page.getContent()).allMatch(u -> "B1".equals(u.getBatch()));
    }

    @Test
    void findByBatch_returnsEmptyForUnknownBatch() {
        var page = userRepository.findByBatch("B99", PageRequest.of(0, 10));
        assertThat(page.getContent()).isEmpty();
    }

    @Test
    void findByBatch_respectsPagination() {
        var page = userRepository.findByBatch("B1", PageRequest.of(0, 1));
        assertThat(page.getContent()).hasSize(1);
        assertThat(page.getTotalElements()).isEqualTo(2);
        assertThat(page.getTotalPages()).isEqualTo(2);
    }

    private User buildUser(String matricule, String email, String batch) {
        return User.builder()
                .matricule(matricule)
                .email(email)
                .name("Test User")
                .passwordHash("hash")
                .status(UserStatus.ACTIVE)
                .batch(batch)
                .build();
    }
}
