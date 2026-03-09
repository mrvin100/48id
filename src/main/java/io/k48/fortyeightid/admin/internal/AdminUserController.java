package io.k48.fortyeightid.admin.internal;

import jakarta.validation.Valid;
import io.k48.fortyeightid.identity.UserStatus;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.DeleteMapping;
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

    @GetMapping
    ResponseEntity<Page<UserResponse>> listUsers(
            @RequestParam(required = false) UserStatus status,
            @RequestParam(required = false) String batch,
            @RequestParam(required = false) String role,
            @PageableDefault(size = 20) Pageable pageable) {
        var page = adminUserService.listUsers(status, batch, role, pageable)
                .map(UserResponse::from);
        return ResponseEntity.ok(page);
    }

    @GetMapping("/{id}")
    ResponseEntity<UserResponse> getUser(@PathVariable UUID id) {
        var user = adminUserService.getUser(id);
        return ResponseEntity.ok(UserResponse.from(user));
    }

    @PutMapping("/{id}")
    ResponseEntity<UserResponse> updateUser(@PathVariable UUID id,
                                            @RequestBody UpdateUserRequest request,
                                            @AuthenticationPrincipal String adminId) {
        var updated = adminUserService.updateUser(id, request, UUID.fromString(adminId));
        return ResponseEntity.ok(UserResponse.from(updated));
    }

    @PutMapping("/{id}/role")
    ResponseEntity<UserResponse> changeRole(@PathVariable UUID id,
                                            @Valid @RequestBody ChangeRoleRequest request,
                                            @AuthenticationPrincipal String adminId) {
        var updated = adminUserService.changeRole(id, request.role(), UUID.fromString(adminId));
        return ResponseEntity.ok(UserResponse.from(updated));
    }

    @PutMapping("/{id}/status")
    ResponseEntity<UserResponse> changeStatus(@PathVariable UUID id,
                                              @Valid @RequestBody ChangeStatusRequest request,
                                              @AuthenticationPrincipal String adminId) {
        var newStatus = UserStatus.valueOf(request.status());
        var updated = adminUserService.changeStatus(id, newStatus, UUID.fromString(adminId));
        return ResponseEntity.ok(UserResponse.from(updated));
    }

    @DeleteMapping("/{id}")
    ResponseEntity<Void> softDeleteUser(@PathVariable UUID id,
                                        @AuthenticationPrincipal String adminId) {
        adminUserService.softDeleteUser(id, UUID.fromString(adminId));
        return ResponseEntity.noContent().build();
    }
}
