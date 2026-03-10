package io.k48.fortyeightid.provisioning.internal;

import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
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

    @PostMapping("/import")
    ResponseEntity<CsvImportResult> importUsers(
            @RequestParam("file") MultipartFile file,
            @AuthenticationPrincipal String adminId) {
        
        try {
            var result = csvImportService.importUsers(file, UUID.fromString(adminId));
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
