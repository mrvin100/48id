package io.k48.fortyeightid.admin.internal;

import jakarta.validation.Valid;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("${fortyeightid.api.prefix}/admin/users")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
class AdminUserController {

    private final AdminUserService adminUserService;

    @GetMapping("/{id}")
    ResponseEntity<UserResponse> getUser(@PathVariable UUID id) {
        var user = adminUserService.getUser(id);
        return ResponseEntity.ok(UserResponse.from(user));
    }

    @PutMapping("/{id}/role")
    ResponseEntity<UserResponse> changeRole(@PathVariable UUID id,
                                            @Valid @RequestBody ChangeRoleRequest request,
                                            @AuthenticationPrincipal String adminId) {
        var updated = adminUserService.changeRole(id, request.role(), UUID.fromString(adminId));
        return ResponseEntity.ok(UserResponse.from(updated));
    }
}
