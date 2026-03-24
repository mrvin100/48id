package io.k48.fortyeightid.shared.exception;

import java.net.URI;
import java.time.Instant;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(AccessDeniedException.class)
    ProblemDetail handleAccessDenied(AccessDeniedException ex) {
        log.warn("Access denied: {}", ex.getMessage());
        var problem = ProblemDetail.forStatusAndDetail(HttpStatus.FORBIDDEN, "You do not have permission to access this resource");
        problem.setTitle("Access Denied");
        problem.setType(URI.create("https://48id.k48.io/errors/access-denied"));
        problem.setProperty("timestamp", Instant.now());
        problem.setProperty("code", "ACCESS_DENIED");
        return problem;
    }

    @ExceptionHandler(BadCredentialsException.class)
    ProblemDetail handleBadCredentials(BadCredentialsException ex) {
        log.warn("Authentication failed: {}", ex.getMessage());
        var problem = ProblemDetail.forStatusAndDetail(HttpStatus.UNAUTHORIZED, ex.getMessage());
        problem.setTitle("Invalid Credentials");
        problem.setType(URI.create("https://48id.k48.io/errors/invalid-credentials"));
        problem.setProperty("timestamp", Instant.now());
        problem.setProperty("code", "INVALID_CREDENTIALS");
        return problem;
    }

    @ExceptionHandler(DisabledException.class)
    ProblemDetail handleDisabled(DisabledException ex) {
        log.warn("Account disabled: {}", ex.getMessage());
        var problem = ProblemDetail.forStatusAndDetail(HttpStatus.UNAUTHORIZED, ex.getMessage());
        problem.setTitle("Account Disabled");
        problem.setType(URI.create("https://48id.k48.io/errors/account-disabled"));
        problem.setProperty("timestamp", Instant.now());
        var code = ex.getMessage().contains("suspended") ? "ACCOUNT_SUSPENDED" : "ACCOUNT_NOT_ACTIVATED";
        problem.setProperty("code", code);
        return problem;
    }

    @ExceptionHandler(AccountLockedException.class)
    ProblemDetail handleAccountLocked(AccountLockedException ex) {
        log.warn("Account locked: {}", ex.getMessage());
        var problem = ProblemDetail.forStatusAndDetail(HttpStatus.UNAUTHORIZED, ex.getMessage());
        problem.setTitle("Account Locked");
        problem.setType(URI.create("https://48id.k48.io/errors/account-locked"));
        problem.setProperty("timestamp", Instant.now());
        problem.setProperty("code", "ACCOUNT_LOCKED");
        problem.setProperty("remainingSeconds", ex.getRemainingSeconds());
        return problem;
    }

    @ExceptionHandler(UserNotFoundException.class)
    ProblemDetail handleUserNotFound(UserNotFoundException ex) {
        log.warn("User not found: {}", ex.getMessage());
        var problem = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
        problem.setTitle("User Not Found");
        problem.setType(URI.create("https://48id.k48.io/errors/user-not-found"));
        problem.setProperty("timestamp", Instant.now());
        return problem;
    }

    @ExceptionHandler(DuplicateMatriculeException.class)
    ProblemDetail handleDuplicateMatricule(DuplicateMatriculeException ex) {
        log.warn("Duplicate matricule: {}", ex.getMessage());
        var problem = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, ex.getMessage());
        problem.setTitle("Duplicate Matricule");
        problem.setType(URI.create("https://48id.k48.io/errors/duplicate-matricule"));
        problem.setProperty("timestamp", Instant.now());
        return problem;
    }

    @ExceptionHandler(DuplicateEmailException.class)
    ProblemDetail handleDuplicateEmail(DuplicateEmailException ex) {
        log.warn("Duplicate email: {}", ex.getMessage());
        var problem = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, ex.getMessage());
        problem.setTitle("Duplicate Email");
        problem.setType(URI.create("https://48id.k48.io/errors/duplicate-email"));
        problem.setProperty("timestamp", Instant.now());
        return problem;
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ProblemDetail handleValidation(MethodArgumentNotValidException ex) {
        log.warn("Validation failed: {}", ex.getMessage());
        var problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, "Validation failed");
        problem.setTitle("Validation Error");
        problem.setType(URI.create("https://48id.k48.io/errors/validation"));
        problem.setProperty("timestamp", Instant.now());
        List<ViolationDetail> violations = ex.getBindingResult().getFieldErrors().stream()
                .map(fe -> new ViolationDetail(fe.getField(), fe.getDefaultMessage()))
                .toList();
        problem.setProperty("violations", violations);
        return problem;
    }

    @ExceptionHandler(PasswordPolicyViolationException.class)
    ProblemDetail handlePasswordPolicyViolation(PasswordPolicyViolationException ex) {
        log.warn("Password policy violation: {}", ex.getMessage());
        var problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, ex.getMessage());
        problem.setTitle("Password Policy Violation");
        problem.setType(URI.create("https://48id.k48.io/errors/password-policy"));
        problem.setProperty("timestamp", Instant.now());
        problem.setProperty("code", "PASSWORD_POLICY_VIOLATION");
        problem.setProperty("violations", ex.getViolations());
        return problem;
    }

    @ExceptionHandler(NewPasswordSameAsCurrentException.class)
    ProblemDetail handleNewPasswordSameAsCurrent(NewPasswordSameAsCurrentException ex) {
        log.warn("New password same as current: {}", ex.getMessage());
        var problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, ex.getMessage());
        problem.setTitle("Password Reuse Not Allowed");
        problem.setType(URI.create("https://48id.k48.io/errors/password-reuse"));
        problem.setProperty("timestamp", Instant.now());
        problem.setProperty("code", "NEW_PASSWORD_SAME_AS_CURRENT");
        return problem;
    }

    @ExceptionHandler(ResetTokenInvalidException.class)
    ProblemDetail handleResetTokenInvalid(ResetTokenInvalidException ex) {
        log.warn("Reset token invalid: {}", ex.getMessage());
        var problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, ex.getMessage());
        problem.setTitle("Reset Token Invalid");
        problem.setType(URI.create("https://48id.k48.io/errors/reset-token-invalid"));
        problem.setProperty("timestamp", Instant.now());
        problem.setProperty("code", "RESET_TOKEN_INVALID");
        return problem;
    }

    @ExceptionHandler(ResetTokenExpiredException.class)
    ProblemDetail handleResetTokenExpired(ResetTokenExpiredException ex) {
        log.warn("Reset token expired: {}", ex.getMessage());
        var problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, ex.getMessage());
        problem.setTitle("Reset Token Expired");
        problem.setType(URI.create("https://48id.k48.io/errors/reset-token-expired"));
        problem.setProperty("timestamp", Instant.now());
        problem.setProperty("code", "RESET_TOKEN_EXPIRED");
        return problem;
    }

    @ExceptionHandler(JwtTokenExpiredException.class)
    ProblemDetail handleJwtExpired(JwtTokenExpiredException ex) {
        log.warn("JWT token expired: {}", ex.getMessage());
        var problem = ProblemDetail.forStatusAndDetail(HttpStatus.UNAUTHORIZED, ex.getMessage());
        problem.setTitle("Token Expired");
        problem.setType(URI.create("https://48id.k48.io/errors/token-expired"));
        problem.setProperty("timestamp", Instant.now());
        problem.setProperty("code", "TOKEN_EXPIRED");
        return problem;
    }

    @ExceptionHandler(JwtSignatureException.class)
    ProblemDetail handleJwtSignature(JwtSignatureException ex) {
        log.warn("JWT signature invalid: {}", ex.getMessage());
        var problem = ProblemDetail.forStatusAndDetail(HttpStatus.UNAUTHORIZED, ex.getMessage());
        problem.setTitle("Token Invalid");
        problem.setType(URI.create("https://48id.k48.io/errors/token-invalid"));
        problem.setProperty("timestamp", Instant.now());
        problem.setProperty("code", "TOKEN_INVALID");
        return problem;
    }

    @ExceptionHandler(RefreshTokenInvalidException.class)
    ProblemDetail handleRefreshTokenInvalid(RefreshTokenInvalidException ex) {
        log.warn("Refresh token invalid: {}", ex.getMessage());
        var problem = ProblemDetail.forStatusAndDetail(HttpStatus.UNAUTHORIZED, ex.getMessage());
        problem.setTitle("Refresh Token Invalid");
        problem.setType(URI.create("https://48id.k48.io/errors/refresh-token-invalid"));
        problem.setProperty("timestamp", Instant.now());
        problem.setProperty("code", "REFRESH_TOKEN_INVALID");
        return problem;
    }

    @ExceptionHandler(MatriculeImmutableException.class)
    ProblemDetail handleMatriculeImmutable(MatriculeImmutableException ex) {
        log.warn("Matricule immutable: {}", ex.getMessage());
        var problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, ex.getMessage());
        problem.setTitle("Matricule Immutable");
        problem.setType(URI.create("https://48id.k48.io/errors/matricule-immutable"));
        problem.setProperty("timestamp", Instant.now());
        problem.setProperty("code", "MATRICULE_IMMUTABLE");
        return problem;
    }

    @ExceptionHandler(CannotDeleteOwnAccountException.class)
    ProblemDetail handleCannotDeleteOwnAccount(CannotDeleteOwnAccountException ex) {
        log.warn("Cannot delete own account: {}", ex.getMessage());
        var problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, ex.getMessage());
        problem.setTitle("Cannot Delete Own Account");
        problem.setType(URI.create("https://48id.k48.io/errors/cannot-delete-own-account"));
        problem.setProperty("timestamp", Instant.now());
        problem.setProperty("code", "CANNOT_DELETE_OWN_ACCOUNT");
        return problem;
    }

    @ExceptionHandler(CannotChangeOwnRoleException.class)
    ProblemDetail handleCannotChangeOwnRole(CannotChangeOwnRoleException ex) {
        log.warn("Cannot change own role: {}", ex.getMessage());
        var problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, ex.getMessage());
        problem.setTitle("Cannot Change Own Role");
        problem.setType(URI.create("https://48id.k48.io/errors/cannot-change-own-role"));
        problem.setProperty("timestamp", Instant.now());
        problem.setProperty("code", "CANNOT_CHANGE_OWN_ROLE");
        return problem;
    }

    @ExceptionHandler(CannotPromoteSuspendedUserException.class)
    ProblemDetail handleCannotPromoteSuspended(CannotPromoteSuspendedUserException ex) {
        log.warn("Cannot promote suspended user: {}", ex.getMessage());
        var problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, ex.getMessage());
        problem.setTitle("Cannot Promote Suspended User");
        problem.setType(URI.create("https://48id.k48.io/errors/cannot-promote-suspended-user"));
        problem.setProperty("timestamp", Instant.now());
        problem.setProperty("code", "CANNOT_PROMOTE_SUSPENDED_USER");
        return problem;
    }

    @ExceptionHandler(OperatorAccountNameTakenException.class)
    ProblemDetail handleOperatorAccountNameTaken(OperatorAccountNameTakenException ex) {
        log.warn("Operator account name taken: {}", ex.getMessage());
        var problem = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, ex.getMessage());
        problem.setTitle("Operator Account Name Taken");
        problem.setType(URI.create("https://48id.k48.io/errors/operator-account-name-taken"));
        problem.setProperty("timestamp", Instant.now());
        problem.setProperty("code", "OPERATOR_ACCOUNT_NAME_TAKEN");
        return problem;
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    ProblemDetail handleDataIntegrity(DataIntegrityViolationException ex) {
        String msg = ex.getMostSpecificCause().getMessage();
        if (msg != null && msg.contains("operator_accounts") && msg.contains("name")) {
            log.warn("Operator account name conflict (race condition): {}", msg);
            var problem = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, "An operator account with this name already exists");
            problem.setTitle("Operator Account Name Taken");
            problem.setType(URI.create("https://48id.k48.io/errors/operator-account-name-taken"));
            problem.setProperty("timestamp", Instant.now());
            problem.setProperty("code", "OPERATOR_ACCOUNT_NAME_TAKEN");
            return problem;
        }
        log.error("Data integrity violation", ex);
        var problem = ProblemDetail.forStatusAndDetail(HttpStatus.INTERNAL_SERVER_ERROR, "A data integrity error occurred");
        problem.setTitle("Internal Server Error");
        problem.setType(URI.create("https://48id.k48.io/errors/internal"));
        problem.setProperty("timestamp", Instant.now());
        return problem;
    }

    @ExceptionHandler(Exception.class)
    ProblemDetail handleUnexpected(Exception ex) {
        log.error("Unexpected error", ex);
        var problem = ProblemDetail.forStatusAndDetail(HttpStatus.INTERNAL_SERVER_ERROR, "An unexpected error occurred");
        problem.setTitle("Internal Server Error");
        problem.setType(URI.create("https://48id.k48.io/errors/internal"));
        problem.setProperty("timestamp", Instant.now());
        return problem;
    }

    record ViolationDetail(String field, String message) {}
}
