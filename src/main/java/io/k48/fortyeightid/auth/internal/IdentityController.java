package io.k48.fortyeightid.auth.internal;

import io.k48.fortyeightid.identity.UserQueryService;
import io.k48.fortyeightid.identity.UserStatus;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("${fortyeightid.api.prefix}/auth")
@RequiredArgsConstructor
class IdentityController {

    private final JwtTokenService jwtTokenService;
    private final UserQueryService userQueryService;

    @PostMapping("/verify-token")
    ResponseEntity<VerifyTokenResponse> verifyToken(@RequestBody VerifyTokenRequest request) {
        try {
            // Validate JWT signature and expiration
            Jwt jwt = jwtTokenService.validateToken(request.token());

            // Extract user ID from token subject
            var userId = UUID.fromString(jwt.getSubject());

            // Get user from database
            var user = userQueryService.findById(userId)
                    .orElseThrow(() -> new IllegalArgumentException("User not found"));

            // Check if user is suspended
            if (user.getStatus() == UserStatus.SUSPENDED) {
                return ResponseEntity.ok(VerifyTokenResponse.invalid("ACCOUNT_SUSPENDED"));
            }

            // Build user info from JWT claims
            var userInfo = new VerifyTokenResponse.UserInfo(
                    user.getId().toString(),
                    user.getMatricule(),
                    user.getName(),
                    user.getEmail(),
                    jwt.getClaimAsString("role"),
                    user.getBatch() != null ? user.getBatch() : "",
                    user.getSpecialization() != null ? user.getSpecialization() : ""
            );

            return ResponseEntity.ok(VerifyTokenResponse.valid(userInfo));

        } catch (io.k48.fortyeightid.shared.exception.JwtTokenExpiredException e) {
            return ResponseEntity.ok(VerifyTokenResponse.invalid("TOKEN_EXPIRED"));
        } catch (io.k48.fortyeightid.shared.exception.JwtSignatureException e) {
            return ResponseEntity.ok(VerifyTokenResponse.invalid("TOKEN_INVALID"));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.ok(VerifyTokenResponse.invalid("USER_NOT_FOUND"));
        } catch (Exception e) {
            return ResponseEntity.ok(VerifyTokenResponse.invalid("TOKEN_INVALID"));
        }
    }

    @GetMapping("/users/{id}/identity")
    ResponseEntity<PublicIdentityResponse> getIdentity(@PathVariable UUID id) {
        var user = userQueryService.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        // Suspended users are treated as non-existent (privacy protection)
        if (user.getStatus() == UserStatus.SUSPENDED) {
            throw new IllegalArgumentException("User not found");
        }

        var response = new PublicIdentityResponse(
                user.getId().toString(),
                user.getMatricule(),
                user.getName(),
                user.getBatch() != null ? user.getBatch() : "",
                user.getSpecialization() != null ? user.getSpecialization() : "",
                user.isProfileCompleted()
        );

        return ResponseEntity.ok(response);
    }
}
