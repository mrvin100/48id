package io.k48.fortyeightid.identity.internal;

import static org.assertj.core.api.Assertions.assertThat;

import io.k48.fortyeightid.TestcontainersConfiguration;
import io.k48.fortyeightid.identity.User;
import io.k48.fortyeightid.identity.UserStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import io.k48.fortyeightid.shared.config.JpaAuditingConfig;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageRequest;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({TestcontainersConfiguration.class, JpaAuditingConfig.class})
class UserRepositoryTest {

    @Autowired
    private UserRepository userRepository;

    private User savedUser;

    @BeforeEach
    void setUp() {
        userRepository.deleteAll();
        savedUser = userRepository.save(User.builder()
                .matricule("K48-B1-1")
                .email("test@k48.io")
                .name("Test User")
                .passwordHash("$2a$10$hashedpassword")
                .status(UserStatus.ACTIVE)
                .batch("2024")
                .build());
    }

    @Test
    void findByMatricule_returnsUser() {
        var result = userRepository.findByMatricule("K48-B1-1");
        assertThat(result).isPresent();
        assertThat(result.get().getEmail()).isEqualTo("test@k48.io");
    }

    @Test
    void findByMatricule_returnsEmptyForNonExistent() {
        assertThat(userRepository.findByMatricule("NONEXISTENT")).isEmpty();
    }

    @Test
    void findByEmail_returnsUser() {
        var result = userRepository.findByEmail("test@k48.io");
        assertThat(result).isPresent();
        assertThat(result.get().getMatricule()).isEqualTo("K48-B1-1");
    }

    @Test
    void existsByMatricule_returnsTrue() {
        assertThat(userRepository.existsByMatricule("K48-B1-1")).isTrue();
    }

    @Test
    void existsByEmail_returnsFalse() {
        assertThat(userRepository.existsByEmail("nobody@k48.io")).isFalse();
    }

    @Test
    void findByStatus_returnsPage() {
        var page = userRepository.findByStatus(UserStatus.ACTIVE, PageRequest.of(0, 10));
        assertThat(page.getContent()).hasSize(1);
    }

    @Test
    void findByBatch_returnsPage() {
        var page = userRepository.findByBatch("2024", PageRequest.of(0, 10));
        assertThat(page.getContent()).hasSize(1);
    }
}
