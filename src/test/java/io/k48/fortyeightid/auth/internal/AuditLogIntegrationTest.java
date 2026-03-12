package io.k48.fortyeightid.auth.internal;

import static org.assertj.core.api.Assertions.assertThat;

import io.k48.fortyeightid.Application;
import io.k48.fortyeightid.audit.internal.AuditLog;
import io.k48.fortyeightid.audit.internal.AuditLogRepository;
import io.k48.fortyeightid.identity.User;
import io.k48.fortyeightid.identity.UserStatus;
import io.k48.fortyeightid.identity.internal.UserRepository;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@SpringBootTest(classes = Application.class)
@Testcontainers
@ActiveProfiles("test")
class AuditLogIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15-alpine");

    @DynamicPropertySource
    static void configureTestProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @Autowired
    private AuditLogRepository auditLogRepository;

    @Autowired
    private AuthService authService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @BeforeEach
    void setUp() {
        auditLogRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    void loginSuccess_writesAuditLog() {
        // Given: A user exists in the database
        var user = createUser("K48-2024-001", "test@k48.io", "TestUser");
        userRepository.save(user);

        // When: User logs in successfully
        var request = new LoginRequest("K48-2024-001", "TestPass@123");
        authService.login(request);

        // Then: LOGIN_SUCCESS audit log entry exists
        var auditLogs = auditLogRepository.findAll();
        assertThat(auditLogs).hasSize(1);
        
        var log = auditLogs.get(0);
        assertThat(log.getAction()).isEqualTo("LOGIN_SUCCESS");
        assertThat(log.getUserId()).isEqualTo(user.getId());
        assertThat(log.getIpAddress()).isNotNull();
    }

    @Test
    void loginFailure_writesAuditLog() {
        // Given: A user exists in the database
        var user = createUser("K48-2024-001", "test@k48.io", "TestUser");
        userRepository.save(user);

        // When: User attempts login with wrong password
        var request = new LoginRequest("K48-2024-001", "WrongPass@123");
        try {
            authService.login(request);
        } catch (Exception e) {
            // Expected - login should fail
        }

        // Then: LOGIN_FAILURE audit log entry exists
        var auditLogs = auditLogRepository.findAll();
        assertThat(auditLogs).hasSize(1);
        
        var log = auditLogs.get(0);
        assertThat(log.getAction()).isEqualTo("LOGIN_FAILURE");
        assertThat(log.getUserId()).isNull(); // User not found yet
    }

    @Test
    void loginFailureForSuspendedUser_writesAuditLog() {
        // Given: A suspended user exists
        var user = createUser("K48-2024-001", "test@k48.io", "TestUser");
        user.setStatus(UserStatus.SUSPENDED);
        userRepository.save(user);

        // When: Suspended user attempts login
        var request = new LoginRequest("K48-2024-001", "TestPass@123");
        try {
            authService.login(request);
        } catch (Exception e) {
            // Expected - login should fail
        }

        // Then: Audit log entry exists
        var auditLogs = auditLogRepository.findAll();
        assertThat(auditLogs).hasSize(1);
        
        var log = auditLogs.get(0);
        assertThat(log.getAction()).isEqualTo("LOGIN_FAILURE");
    }

    private User createUser(String matricule, String email, String name) {
        return User.builder()
                .matricule(matricule)
                .email(email)
                .name(name)
                .passwordHash(passwordEncoder.encode("TestPass@123"))
                .status(UserStatus.ACTIVE)
                .build();
    }
}
