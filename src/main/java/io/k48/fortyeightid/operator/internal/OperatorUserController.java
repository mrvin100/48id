package io.k48.fortyeightid.operator.internal;

import io.k48.fortyeightid.admin.DashboardQueryPort;
import io.k48.fortyeightid.identity.UserStatus;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("${fortyeightid.api.prefix}/operator/users")
@PreAuthorize("hasRole('OPERATOR')")
@RequiredArgsConstructor
class OperatorUserController {

    private final DashboardQueryPort dashboardQueryPort;

    @GetMapping
    ResponseEntity<Page<OperatorUserResponse>> listUsers(
            @RequestParam(required = false) UserStatus status,
            @RequestParam(required = false) String batch,
            @RequestParam(required = false) String role,
            @PageableDefault(size = 20) Pageable pageable) {
        var page = dashboardQueryPort.listUsers(status, batch, role, pageable)
                .map(OperatorUserResponse::from);
        return ResponseEntity.ok(page);
    }

    @GetMapping("/{id}")
    ResponseEntity<OperatorUserResponse> getUser(@PathVariable UUID id) {
        var user = dashboardQueryPort.getUser(id);
        return ResponseEntity.ok(OperatorUserResponse.from(user));
    }
}
