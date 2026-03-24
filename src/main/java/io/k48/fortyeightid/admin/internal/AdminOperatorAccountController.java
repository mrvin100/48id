package io.k48.fortyeightid.admin.internal;

import io.k48.fortyeightid.audit.AuditService;
import io.k48.fortyeightid.operator.OperatorAccountPort;
import io.k48.fortyeightid.operator.OperatorAccountPort.CreateOperatorAccountCommand;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("${fortyeightid.api.prefix}/admin/operator-accounts")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
class AdminOperatorAccountController {

    private final OperatorAccountPort operatorAccountPort;
    private final AuditService auditService;

    @PostMapping
    ResponseEntity<OperatorAccountResponse> createAccount(
            @Valid @RequestBody CreateOperatorAccountRequest request,
            @AuthenticationPrincipal String adminId) {

        var adminUuid = UUID.fromString(adminId);
        var created = operatorAccountPort.createAccount(
            new CreateOperatorAccountCommand(request.name(), request.description(), adminUuid));

        auditService.log(adminUuid, "OPERATOR_ACCOUNT_CREATED", Map.of(
            "accountId", created.accountId().toString(),
            "accountName", created.accountName(),
            "createdBy", adminUuid.toString()
        ));

        return ResponseEntity
            .created(URI.create("/api/v1/admin/operator-accounts/" + created.accountId()))
            .body(OperatorAccountResponse.from(created));
    }
}
