package io.k48.fortyeightid.provisioning.internal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import io.k48.fortyeightid.TestcontainersConfiguration;
import io.k48.fortyeightid.auth.EmailPort;
import io.k48.fortyeightid.identity.UserStatus;
import io.k48.fortyeightid.identity.internal.RoleRepository;
import io.k48.fortyeightid.identity.internal.UserRepository;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

/**
 * End-to-end integration test for the activation flow:
 * CSV import → user created as PENDING_ACTIVATION → activation email sent
 */
@SpringBootTest
@ActiveProfiles("test")
@Import(TestcontainersConfiguration.class)
@Transactional
class ActivationFlowIntegrationTest {

    @Autowired
    private CsvImportService csvImportService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @MockBean
    private EmailPort emailService;

    @Test
    void csvImport_createsUserAsPendingAndTriggersActivationEmail() {
        var csv = """
                matricule,email,name,phone,batch,specialization
                K48-2024-FLOW-001,flow@k48.io,Flow User,+237600000000,2024,Software Engineering
                """;
        var file = new MockMultipartFile("file", "users.csv", "text/csv", csv.getBytes());
        var adminId = UUID.randomUUID();

        var result = csvImportService.importUsers(file, adminId);

        assertThat(result.imported()).isEqualTo(1);
        assertThat(result.failed()).isEqualTo(0);

        var user = userRepository.findByMatricule("K48-2024-FLOW-001").orElseThrow();
        assertThat(user.getStatus()).isEqualTo(UserStatus.PENDING_ACTIVATION);
        assertThat(user.isRequiresPasswordChange()).isTrue();

        verify(emailService, times(1)).sendActivationEmail(
                eq("flow@k48.io"),
                eq("Flow User"),
                eq("K48-2024-FLOW-001"),
                anyString(),
                anyString()
        );
    }

    @Test
    void multipleImports_eachTriggersActivationEmail() {
        var csv = """
                matricule,email,name,phone,batch,specialization
                K48-2024-FLOW-002,user1@k48.io,User One,+123,2024,SE
                K48-2024-FLOW-003,user2@k48.io,User Two,+456,2024,DA
                """;
        var file = new MockMultipartFile("file", "users.csv", "text/csv", csv.getBytes());
        var adminId = UUID.randomUUID();

        var result = csvImportService.importUsers(file, adminId);

        assertThat(result.imported()).isEqualTo(2);
        verify(emailService, times(2)).sendActivationEmail(anyString(), anyString(), anyString(), anyString(), anyString());

        var user1 = userRepository.findByMatricule("K48-2024-FLOW-002").orElseThrow();
        var user2 = userRepository.findByMatricule("K48-2024-FLOW-003").orElseThrow();
        assertThat(user1.getStatus()).isEqualTo(UserStatus.PENDING_ACTIVATION);
        assertThat(user2.getStatus()).isEqualTo(UserStatus.PENDING_ACTIVATION);
    }
}
