package io.k48.fortyeightid.auth.internal;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("${fortyeightid.api.prefix}/auth")
@RequiredArgsConstructor
@Tag(name = "Authentication", description = "User authentication and token management endpoints")
class AuthController {

    private final AuthService authService;
    private final PasswordResetService passwordResetService;
    private final BootstrapService bootstrapService;

    @PostMapping("/login")
    @Operation(summary = "User login", description = "Authenticate user with matricule and password. Returns JWT access token and refresh token.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Login successful", content = @Content(schema = @Schema(implementation = LoginResponse.class))),
        @ApiResponse(responseCode = "400", description = "Invalid request body", content = @Content(schema = @Schema(example = "{\"type\":\"...\",\"detail\":\"Validation failed\"}"))),
        @ApiResponse(responseCode = "401", description = "Invalid credentials", content = @Content(schema = @Schema(example = "{\"type\":\"...\",\"detail\":\"Matricule or password is incorrect.\"}")))
    })
    ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }

    @PostMapping("/refresh")
    @Operation(summary = "Refresh access token", description = "Exchange refresh token for new access token. Rotates refresh token.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Token refreshed successfully", content = @Content(schema = @Schema(implementation = RefreshResponse.class))),
        @ApiResponse(responseCode = "401", description = "Invalid or expired refresh token", content = @Content(schema = @Schema(example = "{\"type\":\"...\",\"detail\":\"Refresh token invalid\"}")))
    })
    ResponseEntity<RefreshResponse> refresh(@Valid @RequestBody RefreshRequest request) {
        return ResponseEntity.ok(authService.refresh(request));
    }

    @PostMapping("/logout")
    @Operation(summary = "User logout", description = "Revoke refresh token and terminate session.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "204", description = "Logout successful"),
        @ApiResponse(responseCode = "401", description = "Invalid refresh token")
    })
    ResponseEntity<Void> logout(@Valid @RequestBody LogoutRequest request) {
        authService.logout(request);
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }

    @PostMapping("/change-password")
    @Operation(summary = "Change password", description = "Authenticated user changes their password. Requires current password.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Password changed successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid current password or new password doesn't meet policy", content = @Content(schema = @Schema(example = "{\"type\":\"...\",\"detail\":\"Password does not meet policy requirements\"}"))),
        @ApiResponse(responseCode = "401", description = "Invalid current password or not authenticated")
    })
    ResponseEntity<Void> changePassword(@Parameter(hidden = true) @AuthenticationPrincipal String userId,
                                        @Valid @RequestBody ChangePasswordRequest request) {
        authService.changePassword(UUID.fromString(userId), request);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/forgot-password")
    @Operation(summary = "Request password reset", description = "Send password reset email to user. Always returns 200 to prevent email enumeration.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "If email is registered, reset email sent", content = @Content(schema = @Schema(implementation = ForgotPasswordResponse.class))),
        @ApiResponse(responseCode = "400", description = "Invalid email format")
    })
    ResponseEntity<ForgotPasswordResponse> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        passwordResetService.handleForgotPassword(request.email());
        return ResponseEntity.ok(new ForgotPasswordResponse("If this email is registered, a password reset link has been sent."));
    }

    @PostMapping("/reset-password")
    @Operation(summary = "Reset password", description = "Reset password using token from email. Invalidates all refresh tokens.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Password reset successful", content = @Content(schema = @Schema(implementation = ResetPasswordResponse.class))),
        @ApiResponse(responseCode = "400", description = "Invalid or expired token, or password doesn't meet policy", content = @Content(schema = @Schema(example = "{\"type\":\"...\",\"detail\":\"This reset link has expired\"}")))
    })
    ResponseEntity<ResetPasswordResponse> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        passwordResetService.resetPassword(request.token(), request.newPassword());
        return ResponseEntity.ok(new ResetPasswordResponse("Password reset successful. Please log in with your new password."));
    }

    @PostMapping("/activate-account")
    @Operation(summary = "Activate account", description = "Activate a provisioned account using the activation token from email.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Account activated successfully", content = @Content(schema = @Schema(implementation = ActivateAccountResponse.class))),
        @ApiResponse(responseCode = "400", description = "Invalid or expired activation token"),
        @ApiResponse(responseCode = "401", description = "Account cannot be activated in its current state")
    })
    ResponseEntity<ActivateAccountResponse> activateAccount(@Valid @RequestBody ActivateAccountRequest request) {
        passwordResetService.activateAccount(request.token());
        return ResponseEntity.ok(new ActivateAccountResponse("Account activated successfully. You can now log in with your temporary password and change it on first login."));
    }

    @PostMapping("/bootstrap")
    @Operation(summary = "Create first admin user", description = "Bootstrap endpoint to create the first admin user when no admin exists. This endpoint automatically disables itself after first use.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "201", description = "First admin user created successfully", content = @Content(schema = @Schema(implementation = BootstrapResponse.class))),
        @ApiResponse(responseCode = "400", description = "Invalid request data or validation failed"),
        @ApiResponse(responseCode = "409", description = "Admin users already exist - bootstrap not available", content = @Content(schema = @Schema(example = "{\"type\":\"...\",\"detail\":\"Cannot create admin user - admin users already exist in the system\"}"))),
        @ApiResponse(responseCode = "422", description = "Matricule or email already exists")
    })
    ResponseEntity<BootstrapResponse> bootstrap(@Valid @RequestBody BootstrapRequest request) {
        var response = bootstrapService.createFirstAdmin(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/bootstrap/available")
    @Operation(summary = "Check bootstrap availability", description = "Check if the bootstrap endpoint is available (i.e., no admin users exist).")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Bootstrap availability status", content = @Content(schema = @Schema(example = "{\"available\": true}")))
    })
    ResponseEntity<java.util.Map<String, Boolean>> isBootstrapAvailable() {
        boolean available = bootstrapService.isBootstrapAvailable();
        return ResponseEntity.ok(java.util.Map.of("available", available));
    }
}
