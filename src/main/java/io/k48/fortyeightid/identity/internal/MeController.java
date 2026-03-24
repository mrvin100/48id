package io.k48.fortyeightid.identity.internal;

import io.k48.fortyeightid.identity.UserQueryService;
import io.k48.fortyeightid.shared.exception.UserNotFoundException;
import jakarta.validation.Valid;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("${fortyeightid.api.prefix}/me")
@RequiredArgsConstructor
class MeController {

    private final UserQueryService userQueryService;
    private final UserService userService;

    @GetMapping
    ResponseEntity<MeResponse> me(@AuthenticationPrincipal String userId) {
        var user = userQueryService.findById(UUID.fromString(userId))
                .orElseThrow(() -> new UserNotFoundException("User not found: " + userId));
        return ResponseEntity.ok(MeResponse.from(user));
    }

    @PutMapping
    ResponseEntity<MeResponse> updateProfile(
            @AuthenticationPrincipal String userId,
            @Valid @RequestBody UpdateProfileRequest request) {
        var updated = userService.updateProfile(UUID.fromString(userId), request);
        return ResponseEntity.ok(MeResponse.from(updated));
    }
}
