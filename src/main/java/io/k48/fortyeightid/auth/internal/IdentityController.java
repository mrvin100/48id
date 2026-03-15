package io.k48.fortyeightid.auth.internal;

import io.k48.fortyeightid.identity.UserQueryService;
import io.k48.fortyeightid.identity.UserStatus;
import jakarta.validation.Valid;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("${fortyeightid.api.prefix}")
@RequiredArgsConstructor
class IdentityController {

    private final JwtTokenService jwtTokenService;
    private final UserQueryService userQueryService;

    @PostMapping("/auth/verify-token")
    @PreAuthorize("hasRole('API_CLIENT')")
    ResponseEntity<VerifyTokenResponse> verifyToken(@Valid @RequestBody VerifyTokenRequest request) {
        try {
            Jwt jwt = jwtTokenService.validateTokenInternal(request.token());
            var userId = UUID.fromString(jwt.getSubject());
            var user = userQueryService.findById(userId).orElseThrow(() -> new IllegalArgumentException("User not found"));

            if (user.getStatus() == UserStatus.SUSPENDED) {
                return ResponseEntity.ok(VerifyTokenResponse.invalid("ACCOUNT_SUSPENDED"));
            }

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
    @PreAuthorize("hasRole('API_CLIENT')")
    ResponseEntity<PublicIdentityResponse> getPublicIdentity(@PathVariable UUID id) {
        var user = userQueryService.findById(id)
                .orElseThrow(() -> new io.k48.fortyeightid.shared.exception.UserNotFoundException("User not found: " + id));
        return ResponseEntity.ok(new PublicIdentityResponse(
                user.getId().toString(),
                user.getMatricule(),
                user.getName(),
                user.getBatch(),
                user.getSpecialization(),
                user.isProfileCompleted()
        ));
    }

    @GetMapping("/users/{matricule}/exists")
    @PreAuthorize("hasRole('API_CLIENT')")
    ResponseEntity<MatriculeExistsResponse> matriculeExists(@PathVariable String matricule) {
        return userQueryService.findByMatricule(matricule)
                .map(user -> ResponseEntity.ok(MatriculeExistsResponse.exists(user.getStatus().name())))
                .orElseGet(() -> ResponseEntity.ok(MatriculeExistsResponse.notExists()));
    }
}
