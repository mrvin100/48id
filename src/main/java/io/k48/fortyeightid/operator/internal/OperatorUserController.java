package io.k48.fortyeightid.operator.internal;

import java.time.Instant;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Returns 48ID users who have authenticated externally via this operator account's API key.
 * These are the "consumers" of the operator's platform — not account members.
 *
 * Caller must be an active member of the account.
 */
@RestController
@RequestMapping("${fortyeightid.api.prefix}/operator/users")
@PreAuthorize("hasRole('OPERATOR')")
@RequiredArgsConstructor
class OperatorUserController {

    private final OperatorApiConsumerService operatorApiConsumerService;
    private final OperatorAccountService operatorAccountService;

    @GetMapping
    ResponseEntity<Page<ApiConsumerResponse>> listApiConsumers(
            @RequestParam UUID accountId,
            @PageableDefault(size = 20) Pageable pageable,
            @AuthenticationPrincipal String callerId) {

        operatorAccountService.requireActiveMember(accountId, UUID.fromString(callerId));

        var page = operatorApiConsumerService
                .listApiConsumers(accountId, pageable)
                .map(ApiConsumerResponse::from);
        return ResponseEntity.ok(page);
    }

    record ApiConsumerResponse(
            UUID userId,
            String matricule,
            String email,
            String name,
            String batch,
            String status,
            long totalCalls,
            Instant firstSeen,
            Instant lastSeen
    ) {
        static ApiConsumerResponse from(OperatorApiConsumerService.ApiConsumerSummary s) {
            return new ApiConsumerResponse(
                    s.userId(), s.matricule(), s.email(), s.name(),
                    s.batch(), s.status(), s.totalCalls(), s.firstSeen(), s.lastSeen()
            );
        }
    }
}
