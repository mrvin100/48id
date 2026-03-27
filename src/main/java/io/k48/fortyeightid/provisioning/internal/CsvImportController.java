package io.k48.fortyeightid.provisioning.internal;

import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("${fortyeightid.api.prefix}/admin/users")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
class CsvImportController {

    private final CsvImportService csvImportService;

    @GetMapping("/import/template")
    ResponseEntity<byte[]> downloadTemplate() {
        var csvContent = csvImportService.generateTemplate();
        
        var headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType("text/csv"));
        headers.setContentDispositionFormData("attachment", "48id_import_template.csv");
        
        return ResponseEntity.ok()
                .headers(headers)
                .body(csvContent.getBytes(java.nio.charset.StandardCharsets.UTF_8));
    }

    @PostMapping("/import")
    ResponseEntity<CsvImportResult> importUsers(
            @RequestParam("file") MultipartFile file,
            @AuthenticationPrincipal String adminId) {

        // CsvImportException is intentionally handled here to return CsvImportResult format
        // instead of ProblemDetail (which GlobalCsvExceptionHandler would produce).
        // This provides a consistent response format for both validation and import errors.
        try {
            UUID parsedAdminId;
            try {
                parsedAdminId = UUID.fromString(adminId);
            } catch (IllegalArgumentException e) {
                return ResponseEntity.badRequest().body(
                        new CsvImportResult(0, 1, java.util.List.of(
                                new CsvRowError(0, "", "INVALID_ADMIN_ID: " + e.getMessage())
                        ))
                );
            }

            var result = csvImportService.importUsers(file, parsedAdminId);
            return ResponseEntity.ok(result);
        } catch (CsvImportService.CsvImportException e) {
            return ResponseEntity.badRequest().body(
                    new CsvImportResult(0, 0, java.util.List.of(
                            new CsvRowError(0, "", e.getErrorCode() + ": " + e.getMessage())
                    ))
            );
        }
    }
}
