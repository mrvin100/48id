package io.k48.fortyeightid.provisioning.internal;

import com.opencsv.CSVReader;
import com.opencsv.exceptions.CsvException;
import io.k48.fortyeightid.audit.AuditService;
import io.k48.fortyeightid.auth.PasswordResetPort;
import io.k48.fortyeightid.identity.UserProvisioningPort;
import io.k48.fortyeightid.shared.exception.DuplicateEmailException;
import io.k48.fortyeightid.shared.exception.DuplicateMatriculeException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Pattern;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Slf4j
@Service
@RequiredArgsConstructor
class CsvImportService {

    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");
    private static final List<String> EXPECTED_HEADER = List.of("matricule", "email", "name", "phone", "batch", "specialization");
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private final UserProvisioningPort userProvisioningService;
    private final PasswordResetPort passwordResetService;
    private final AuditService auditService;

    String generateTemplate() {
        var header = String.join(",", EXPECTED_HEADER);
        var exampleRow = "K48-B1-1,john.doe@k48.io,John Doe,+237600000000,B1,Software Engineering";
        return header + "\n" + exampleRow + "\n";
    }

    CsvImportResult importUsers(MultipartFile file, UUID adminId) {
        try {
            validateFileFormat(file);
            var rows = parseAndValidateCsv(file);

            if (rows.isEmpty()) {
                throw new CsvImportException("CSV_NO_DATA_ROWS", "CSV file contains no data rows");
            }

            return processImport(rows, adminId);
        } catch (IOException | CsvException e) {
            log.error("Failed to parse CSV file: {}", e.getMessage(), e);
            throw new CsvImportException("INVALID_FILE_FORMAT", "Failed to parse CSV file: " + e.getMessage());
        }
    }

    private void validateFileFormat(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new CsvImportException("INVALID_FILE_FORMAT", "File is empty or missing");
        }

        var contentType = file.getContentType();
        var filename = file.getOriginalFilename();
        var looksLikeCsvByMime = contentType == null
                || contentType.contains("csv")
                || contentType.contains("text")
                || contentType.contains("application/vnd.ms-excel");
        var looksLikeCsvByName = filename != null && filename.toLowerCase().endsWith(".csv");

        if (!looksLikeCsvByMime && !looksLikeCsvByName) {
            throw new CsvImportException("INVALID_FILE_FORMAT", "File must be a CSV file");
        }
    }

    private List<CsvRow> parseAndValidateCsv(MultipartFile file) throws IOException, CsvException {
        try (var reader = new CSVReader(new InputStreamReader(file.getInputStream()))) {
            var allRows = reader.readAll();

            if (allRows.isEmpty()) {
                throw new CsvImportException("CSV_NO_DATA_ROWS", "CSV file is empty");
            }

            validateHeader(allRows.get(0));

            var rows = new ArrayList<CsvRow>();
            for (int i = 1; i < allRows.size(); i++) {
                var row = allRows.get(i);
                if (row.length == 0 || (row.length == 1 && row[0].isBlank())) {
                    continue;
                }
                rows.add(new CsvRow(
                        i + 1,
                        row.length > 0 ? row[0].trim() : "",
                        row.length > 1 ? row[1].trim() : "",
                        row.length > 2 ? row[2].trim() : "",
                        row.length > 3 ? row[3].trim() : "",
                        row.length > 4 ? row[4].trim() : "",
                        row.length > 5 ? row[5].trim() : ""
                ));
            }

            return rows;
        }
    }

    private CsvImportResult processImport(List<CsvRow> rows, UUID adminId) {
        var errors = new ArrayList<CsvRowError>();
        var successCount = 0;

        for (var row : rows) {
            var validationError = validateRow(row);
            if (validationError != null) {
                errors.add(validationError);
                continue;
            }

            try {
                var tempPassword = generateTemporaryPassword();
                var user = userProvisioningService.createUser(
                        row.matricule(),
                        row.email(),
                        row.name(),
                        row.phone(),
                        row.batch(),
                        row.specialization(),
                        tempPassword
                );

                passwordResetService.initiateActivation(user, tempPassword);
                successCount++;
            } catch (DuplicateMatriculeException e) {
                errors.add(new CsvRowError(row.rowNumber(), row.matricule(), "MATRICULE_ALREADY_EXISTS"));
            } catch (DuplicateEmailException e) {
                errors.add(new CsvRowError(row.rowNumber(), row.matricule(), "EMAIL_ALREADY_EXISTS"));
            } catch (Exception e) {
                log.error("Failed to import row {}: {}", row.rowNumber(), e.getMessage(), e);
                errors.add(new CsvRowError(row.rowNumber(), row.matricule(), "IMPORT_FAILED: " + e.getMessage()));
            }
        }

        auditService.log(adminId, "CSV_IMPORT", Map.of(
                "totalRows", rows.size(),
                "imported", successCount,
                "failed", errors.size()
        ));

        return new CsvImportResult(successCount, errors.size(), errors);
    }

    private CsvRowError validateRow(CsvRow row) {
        if (row.matricule() == null || row.matricule().isBlank()) {
            return new CsvRowError(row.rowNumber(), "", "MISSING_REQUIRED_FIELD: matricule");
        }
        if (row.email() == null || row.email().isBlank()) {
            return new CsvRowError(row.rowNumber(), row.matricule(), "MISSING_REQUIRED_FIELD: email");
        }
        if (row.name() == null || row.name().isBlank()) {
            return new CsvRowError(row.rowNumber(), row.matricule(), "MISSING_REQUIRED_FIELD: name");
        }
        if (row.phone() == null || row.phone().isBlank()) {
            return new CsvRowError(row.rowNumber(), row.matricule(), "MISSING_REQUIRED_FIELD: phone");
        }
        if (row.batch() == null || row.batch().isBlank()) {
            return new CsvRowError(row.rowNumber(), row.matricule(), "MISSING_REQUIRED_FIELD: batch");
        }
        if (row.specialization() == null || row.specialization().isBlank()) {
            return new CsvRowError(row.rowNumber(), row.matricule(), "MISSING_REQUIRED_FIELD: specialization");
        }
        if (!EMAIL_PATTERN.matcher(row.email()).matches()) {
            return new CsvRowError(row.rowNumber(), row.matricule(), "INVALID_EMAIL_FORMAT");
        }
        return null;
    }

    private void validateHeader(String[] headerRow) {
        if (headerRow.length != EXPECTED_HEADER.size()) {
            throw new CsvImportException("INVALID_CSV_HEADER", "CSV header must be exactly: matricule,email,name,phone,batch,specialization");
        }

        for (int i = 0; i < EXPECTED_HEADER.size(); i++) {
            var actual = headerRow[i] == null ? "" : headerRow[i].trim();
            if (!EXPECTED_HEADER.get(i).equals(actual)) {
                throw new CsvImportException("INVALID_CSV_HEADER", "CSV header must be exactly: matricule,email,name,phone,batch,specialization");
            }
        }
    }

    private String generateTemporaryPassword() {
        var bytes = new byte[16];
        SECURE_RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    static class CsvImportException extends RuntimeException {
        private final String errorCode;

        CsvImportException(String errorCode, String message) {
            super(message);
            this.errorCode = errorCode;
        }

        public String getErrorCode() {
            return errorCode;
        }
    }
}
