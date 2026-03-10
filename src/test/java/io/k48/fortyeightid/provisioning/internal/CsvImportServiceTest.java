package io.k48.fortyeightid.provisioning.internal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.k48.fortyeightid.audit.AuditService;
import io.k48.fortyeightid.auth.EmailPort;
import io.k48.fortyeightid.identity.User;
import io.k48.fortyeightid.identity.UserProvisioningPort;
import io.k48.fortyeightid.identity.UserStatus;
import io.k48.fortyeightid.shared.exception.DuplicateMatriculeException;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

@ExtendWith(MockitoExtension.class)
class CsvImportServiceTest {

    @Mock private UserProvisioningPort userProvisioningService;
    @Mock private EmailPort emailService;
    @Mock private AuditService auditService;
    @InjectMocks private CsvImportService csvImportService;

    @Test
    void importUsers_successfullyImportsAllValidRows() {
        var csv = """
                matricule,email,name,phone,batch,specialization
                K48-2024-001,user1@k48.io,User One,+123,2024,SE
                K48-2024-002,user2@k48.io,User Two,+456,2024,DA
                """;
        var file = new MockMultipartFile("file", "users.csv", "text/csv", csv.getBytes());
        var adminId = UUID.randomUUID();

        when(userProvisioningService.createUser(anyString(), anyString(), anyString(), anyString(), anyString(), anyString(), anyString()))
                .thenReturn(User.builder()
                        .matricule("K48-2024-001")
                        .email("user1@k48.io")
                        .name("User One")
                        .status(UserStatus.PENDING_ACTIVATION)
                        .build());

        var result = csvImportService.importUsers(file, adminId);

        assertThat(result.imported()).isEqualTo(2);
        assertThat(result.failed()).isEqualTo(0);
        assertThat(result.errors()).isEmpty();
        verify(userProvisioningService, times(2)).createUser(anyString(), anyString(), anyString(), anyString(), anyString(), anyString(), anyString());
        verify(emailService, times(2)).sendActivationEmail(anyString(), anyString(), anyString(), anyString());
    }

    @Test
    void importUsers_skipsDuplicateMatricules() {
        var csv = """
                matricule,email,name,phone,batch,specialization
                K48-2024-001,user1@k48.io,User One,+123,2024,SE
                K48-2024-002,user2@k48.io,User Two,+456,2024,DA
                """;
        var file = new MockMultipartFile("file", "users.csv", "text/csv", csv.getBytes());
        var adminId = UUID.randomUUID();

        when(userProvisioningService.createUser(eq("K48-2024-001"), anyString(), anyString(), anyString(), anyString(), anyString(), anyString()))
                .thenThrow(new DuplicateMatriculeException("Matricule already exists"));
        when(userProvisioningService.createUser(eq("K48-2024-002"), anyString(), anyString(), anyString(), anyString(), anyString(), anyString()))
                .thenReturn(User.builder().matricule("K48-2024-002").email("user2@k48.io").name("User Two").status(UserStatus.PENDING_ACTIVATION).build());

        var result = csvImportService.importUsers(file, adminId);

        assertThat(result.imported()).isEqualTo(1);
        assertThat(result.failed()).isEqualTo(1);
        assertThat(result.errors()).hasSize(1);
        assertThat(result.errors().get(0).error()).isEqualTo("MATRICULE_ALREADY_EXISTS");
        assertThat(result.errors().get(0).matricule()).isEqualTo("K48-2024-001");
    }

    @Test
    void importUsers_skipsRowsWithMissingRequiredFields() {
        var csv = """
                matricule,email,name,phone,batch,specialization
                K48-2024-001,,User One,+123,2024,SE
                K48-2024-002,user2@k48.io,User Two,+456,2024,DA
                """;
        var file = new MockMultipartFile("file", "users.csv", "text/csv", csv.getBytes());
        var adminId = UUID.randomUUID();

        when(userProvisioningService.createUser(anyString(), anyString(), anyString(), anyString(), anyString(), anyString(), anyString()))
                .thenReturn(User.builder().matricule("K48-2024-002").email("user2@k48.io").name("User Two").status(UserStatus.PENDING_ACTIVATION).build());

        var result = csvImportService.importUsers(file, adminId);

        assertThat(result.imported()).isEqualTo(1);
        assertThat(result.failed()).isEqualTo(1);
        assertThat(result.errors()).hasSize(1);
        assertThat(result.errors().get(0).error()).contains("MISSING_REQUIRED_FIELD: email");
    }

    @Test
    void importUsers_skipsRowsWithInvalidEmailFormat() {
        var csv = """
                matricule,email,name,phone,batch,specialization
                K48-2024-001,invalid-email,User One,+123,2024,SE
                K48-2024-002,user2@k48.io,User Two,+456,2024,DA
                """;
        var file = new MockMultipartFile("file", "users.csv", "text/csv", csv.getBytes());
        var adminId = UUID.randomUUID();

        when(userProvisioningService.createUser(anyString(), anyString(), anyString(), anyString(), anyString(), anyString(), anyString()))
                .thenReturn(User.builder().matricule("K48-2024-002").email("user2@k48.io").name("User Two").status(UserStatus.PENDING_ACTIVATION).build());

        var result = csvImportService.importUsers(file, adminId);

        assertThat(result.imported()).isEqualTo(1);
        assertThat(result.failed()).isEqualTo(1);
        assertThat(result.errors()).hasSize(1);
        assertThat(result.errors().get(0).error()).isEqualTo("INVALID_EMAIL_FORMAT");
    }

    @Test
    void importUsers_throwsExceptionForEmptyCsv() {
        var csv = "matricule,email,name,phone,batch,specialization\n";
        var file = new MockMultipartFile("file", "users.csv", "text/csv", csv.getBytes());
        var adminId = UUID.randomUUID();

        assertThatThrownBy(() -> csvImportService.importUsers(file, adminId))
                .isInstanceOfSatisfying(CsvImportService.CsvImportException.class, ex -> {
                    assertThat(ex.getErrorCode()).isEqualTo("CSV_NO_DATA_ROWS");
                    assertThat(ex.getMessage()).contains("no data rows");
                });

        verify(userProvisioningService, never()).createUser(anyString(), anyString(), anyString(), anyString(), anyString(), anyString(), anyString());
    }

    @Test
    void importUsers_throwsExceptionForNullFile() {
        var adminId = UUID.randomUUID();

        assertThatThrownBy(() -> csvImportService.importUsers(null, adminId))
                .isInstanceOfSatisfying(CsvImportService.CsvImportException.class, ex -> {
                    assertThat(ex.getErrorCode()).isEqualTo("INVALID_FILE_FORMAT");
                    assertThat(ex.getMessage()).contains("empty or missing");
                });
    }
}
