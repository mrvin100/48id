package io.k48.fortyeightid.auth.internal;

import jakarta.validation.Valid;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("${fortyeightid.api.prefix}/auth")
@RequiredArgsConstructor
class AuthController {

    private final AuthService authService;

    @PostMapping("/login")
    ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }

    @PostMapping("/refresh")
    ResponseEntity<RefreshResponse> refresh(@Valid @RequestBody RefreshRequest request) {
        return ResponseEntity.ok(authService.refresh(request));
    }

    @PostMapping("/logout")
    ResponseEntity<Void> logout(@Valid @RequestBody LogoutRequest request) {
        authService.logout(request);
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }

    @PostMapping("/change-password")
    ResponseEntity<Void> changePassword(
            @AuthenticationPrincipal String userId,
            @Valid @RequestBody ChangePasswordRequest request) {
        authService.changePassword(UUID.fromString(userId), request);
        return ResponseEntity.ok().build();
    }
}
