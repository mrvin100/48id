package io.k48.fortyeightid.operator.internal;

import jakarta.validation.Valid;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("${fortyeightid.api.prefix}/auth")
@RequiredArgsConstructor
class OperatorInviteController {

    private final OperatorInviteService operatorInviteService;

    @PostMapping("/accept-operator-invite")
    ResponseEntity<Map<String, Boolean>> acceptOperatorInvite(
            @Valid @RequestBody AcceptOperatorInviteRequest request) {
        operatorInviteService.acceptInviteFlow(request.token(), request.accountId());
        return ResponseEntity.ok(Map.of("success", true));
    }
}
