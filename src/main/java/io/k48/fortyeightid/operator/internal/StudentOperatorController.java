package io.k48.fortyeightid.operator.internal;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
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

/**
 * Student-owned operator account management.
 * Any authenticated user can create an operator account.
 * Only the OWNER of an account can invite/remove members or delete the account.
 */
@RestController
@RequestMapping("${fortyeightid.api.prefix}/operator/accounts")
@PreAuthorize("isAuthenticated()")
@RequiredArgsConstructor
class StudentOperatorController {

    private final OperatorAccountService operatorAccountService;

    /** List all operator accounts the caller is an active member of, with their role in each. */
    @GetMapping
    ResponseEntity<List<MyAccountResponse>> myAccounts(@AuthenticationPrincipal String userId) {
        var accounts = operatorAccountService.listAccountsForUser(UUID.fromString(userId));
        return ResponseEntity.ok(accounts);
    }

    /** Create a new operator account. Caller becomes OWNER and gains OPERATOR role. */
    @PostMapping
    ResponseEntity<AccountResponse> createAccount(@Valid @RequestBody CreateAccountRequest request,
                                                   @AuthenticationPrincipal String userId) {
        var account = operatorAccountService.createAccountForStudent(
                request.name(), request.description(), UUID.fromString(userId));
        return ResponseEntity.status(HttpStatus.CREATED).body(AccountResponse.from(account));
    }

    /** Delete an account. Caller must be OWNER. All members lose OPERATOR role if no other accounts. */
    @DeleteMapping("/{id}")
    ResponseEntity<Void> deleteAccount(@PathVariable UUID id,
                                        @AuthenticationPrincipal String userId) {
        operatorAccountService.deleteAccount(id, UUID.fromString(userId));
        return ResponseEntity.noContent().build();
    }

    /** List members of an account the caller belongs to. */
    @GetMapping("/{id}/members")
    ResponseEntity<List<MemberResponse>> listMembers(@PathVariable UUID id) {
        return ResponseEntity.ok(operatorAccountService.listMembersWithUsers(id));
    }

    /** Invite a student by matricule. Caller must be OWNER. */
    @PostMapping("/{id}/invite")
    ResponseEntity<Void> inviteMember(@PathVariable UUID id,
                                       @Valid @RequestBody InviteByMatriculeRequest request,
                                       @AuthenticationPrincipal String userId) {
        operatorAccountService.inviteMemberByMatricule(id, request.matricule(), UUID.fromString(userId));
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    /** Remove a collaborator. Caller must be OWNER. */
    @DeleteMapping("/{id}/members/{memberId}")
    ResponseEntity<Void> removeMember(@PathVariable UUID id,
                                       @PathVariable UUID memberId,
                                       @AuthenticationPrincipal String userId) {
        operatorAccountService.removeMember(id, memberId, UUID.fromString(userId));
        return ResponseEntity.noContent().build();
    }

    // ── Records ───────────────────────────────────────────────────────────────

    record CreateAccountRequest(
            @NotBlank @Size(max = 100) String name,
            @Size(max = 500) String description) {}

    record InviteByMatriculeRequest(
            @NotBlank @Pattern(regexp = "^K48-B[0-9]{1,4}-[0-9]+$",
                    message = "Matricule must follow format K48-B{n}-{seq}")
            String matricule) {}

    /** Returned by GET /operator/accounts — includes caller's role in each account. */
    record MyAccountResponse(UUID id, String name, String description,
                              UUID ownedApiKeyId, Instant createdAt,
                              String memberRole, String memberStatus) {}

    record AccountResponse(UUID id, String name, String description, UUID ownedApiKeyId, Instant createdAt) {
        static AccountResponse from(OperatorAccount a) {
            return new AccountResponse(a.getId(), a.getName(), a.getDescription(),
                    a.getOwnedApiKeyId(), a.getCreatedAt());
        }
    }

    record MemberResponse(UUID id, UUID userId, String matricule, String name,
                           String memberRole, String status, Instant createdAt) {
        static MemberResponse from(OperatorMembership m, io.k48.fortyeightid.identity.User user) {
            return new MemberResponse(m.getId(), m.getUserId(),
                    user != null ? user.getMatricule() : null,
                    user != null ? user.getName() : null,
                    m.getMemberRole().name(), m.getStatus().name(), m.getCreatedAt());
        }
    }
}
