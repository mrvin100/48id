package io.k48.fortyeightid.operator.internal;

import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

@RestController
@RequestMapping("${fortyeightid.api.prefix}/operator/traffic")
@PreAuthorize("hasRole('OPERATOR')")
@RequiredArgsConstructor
class OperatorTrafficController {

    private final OperatorTrafficService operatorTrafficService;

    @GetMapping
    ResponseEntity<OperatorTrafficResponse> getTraffic(@AuthenticationPrincipal String userId) {
        UUID id;
        try {
            id = UUID.fromString(userId);
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid user identity in token");
        }
        return ResponseEntity.ok(operatorTrafficService.getTrafficForOperator(id));
    }
}
