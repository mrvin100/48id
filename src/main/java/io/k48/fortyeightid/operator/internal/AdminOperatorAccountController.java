package io.k48.fortyeightid.operator.internal;

import io.k48.fortyeightid.admin.TrafficQueryPort;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Admin read-only view of operator accounts.
 * Admins cannot create, delete, or invite — those are student-owned operations.
 */
@RestController
@RequestMapping("${fortyeightid.api.prefix}/admin/operator-accounts")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
class AdminOperatorAccountController {

    private final OperatorAccountService operatorAccountService;
    private final TrafficQueryPort trafficQueryPort;

    @GetMapping
    ResponseEntity<List<AccountResponse>> listAccounts() {
        return ResponseEntity.ok(operatorAccountService.listAllAccounts()
                .stream().map(AccountResponse::from).toList());
    }

    @GetMapping("/{id}")
    ResponseEntity<AccountResponse> getAccount(@PathVariable UUID id) {
        return ResponseEntity.ok(AccountResponse.from(operatorAccountService.getAccount(id)));
    }

    @GetMapping("/{id}/members")
    ResponseEntity<List<MemberResponse>> listMembers(@PathVariable UUID id) {
        return ResponseEntity.ok(operatorAccountService.listMembers(id)
                .stream().map(MemberResponse::from).toList());
    }

    @GetMapping("/{id}/traffic")
    ResponseEntity<TrafficQueryPort.AccountTrafficView> getAccountTraffic(
            @PathVariable UUID id,
            @PageableDefault(size = 50) Pageable pageable) {
        return ResponseEntity.ok(trafficQueryPort.getAccountTrafficDetail(id, pageable));
    }

    record AccountResponse(UUID id, String name, String description, UUID ownedApiKeyId, Instant createdAt) {
        static AccountResponse from(OperatorAccount a) {
            return new AccountResponse(a.getId(), a.getName(), a.getDescription(),
                    a.getOwnedApiKeyId(), a.getCreatedAt());
        }
    }

    record MemberResponse(UUID id, UUID userId, String memberRole, String status, Instant createdAt) {
        static MemberResponse from(OperatorMembership m) {
            return new MemberResponse(m.getId(), m.getUserId(),
                    m.getMemberRole().name(), m.getStatus().name(), m.getCreatedAt());
        }
    }
}
