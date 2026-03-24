package io.k48.fortyeightid.operator.internal;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("${fortyeightid.api.prefix}/admin/operator-accounts")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
class AdminOperatorAccountController {

    private final OperatorAccountService operatorAccountService;

    @GetMapping
    ResponseEntity<List<AccountResponse>> listAccounts() {
        return ResponseEntity.ok(operatorAccountService.listAccounts().stream().map(AccountResponse::from).toList());
    }

    @PostMapping
    ResponseEntity<AccountResponse> createAccount(@Valid @RequestBody CreateAccountRequest request,
                                                   @AuthenticationPrincipal String adminId) {
        var account = operatorAccountService.createAccount(request.name(), request.description(), UUID.fromString(adminId));
        return ResponseEntity.status(HttpStatus.CREATED).body(AccountResponse.from(account));
    }

    @GetMapping("/{id}")
    ResponseEntity<AccountResponse> getAccount(@PathVariable UUID id) {
        return ResponseEntity.ok(AccountResponse.from(operatorAccountService.getAccount(id)));
    }

    @DeleteMapping("/{id}")
    ResponseEntity<Void> deleteAccount(@PathVariable UUID id, @AuthenticationPrincipal String adminId) {
        operatorAccountService.deleteAccount(id, UUID.fromString(adminId));
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}/members")
    ResponseEntity<List<MemberResponse>> listMembers(@PathVariable UUID id) {
        return ResponseEntity.ok(operatorAccountService.listMembers(id).stream().map(MemberResponse::from).toList());
    }

    @PostMapping("/{id}/invite")
    ResponseEntity<Void> inviteMember(@PathVariable UUID id,
                                      @Valid @RequestBody InviteRequest request,
                                      @AuthenticationPrincipal String adminId) {
        operatorAccountService.inviteMember(id, request.userId(), request.role(), UUID.fromString(adminId));
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @DeleteMapping("/{id}/members/{userId}")
    ResponseEntity<Void> removeMember(@PathVariable UUID id, @PathVariable UUID userId,
                                      @AuthenticationPrincipal String adminId) {
        operatorAccountService.removeMember(id, userId, UUID.fromString(adminId));
        return ResponseEntity.noContent().build();
    }

    record CreateAccountRequest(@NotBlank @Size(max = 100) String name, @Size(max = 500) String description) {}

    record InviteRequest(@NotNull UUID userId, @NotBlank String role) {}

    record AccountResponse(UUID id, String name, String description, UUID ownedApiKeyId, Instant createdAt) {
        static AccountResponse from(OperatorAccount a) {
            return new AccountResponse(a.getId(), a.getName(), a.getDescription(), a.getOwnedApiKeyId(), a.getCreatedAt());
        }
    }

    record MemberResponse(UUID id, UUID userId, String memberRole, String status, Instant createdAt) {
        static MemberResponse from(OperatorMembership m) {
            return new MemberResponse(m.getId(), m.getUserId(), m.getMemberRole().name(), m.getStatus().name(), m.getCreatedAt());
        }
    }
}
