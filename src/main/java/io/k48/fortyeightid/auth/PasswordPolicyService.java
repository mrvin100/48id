package io.k48.fortyeightid.auth;

import io.k48.fortyeightid.shared.exception.PasswordPolicyViolationException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import org.springframework.stereotype.Service;

/**
 * Shared password policy validator used by all password-setting flows.
 * Ensures consistent password requirements across:
 * - CSV import (temporary passwords)
 * - Password reset
 * - Password change
 * - Initial user creation
 */
@Service
public class PasswordPolicyService {

    private static final Pattern UPPERCASE_PATTERN = Pattern.compile("[A-Z]");
    private static final Pattern LOWERCASE_PATTERN = Pattern.compile("[a-z]");
    private static final Pattern DIGIT_PATTERN = Pattern.compile("[0-9]");
    private static final Pattern SPECIAL_CHAR_PATTERN = Pattern.compile("[@$!%*?&]");

    private static final int MIN_LENGTH = 8;

    /**
     * Validates a password against the policy.
     * @param password The password to validate
     * @throws PasswordPolicyViolationException if password does not meet policy
     */
    public void validate(String password) {
        var violations = validateWithViolations(password);
        if (!violations.isEmpty()) {
            throw new PasswordPolicyViolationException(violations);
        }
    }

    /**
     * Validates a password and returns a list of violations.
     * @param password The password to validate
     * @return List of violation messages (empty if password is valid)
     */
    public List<String> validateWithViolations(String password) {
        var violations = new ArrayList<String>();

        if (password == null || password.isEmpty()) {
            violations.add("Password is required");
            return violations;
        }

        if (password.length() < MIN_LENGTH) {
            violations.add("Password must be at least " + MIN_LENGTH + " characters long");
        }

        if (!UPPERCASE_PATTERN.matcher(password).find()) {
            violations.add("Password must contain at least one uppercase letter (A-Z)");
        }

        if (!LOWERCASE_PATTERN.matcher(password).find()) {
            violations.add("Password must contain at least one lowercase letter (a-z)");
        }

        if (!DIGIT_PATTERN.matcher(password).find()) {
            violations.add("Password must contain at least one digit (0-9)");
        }

        if (!SPECIAL_CHAR_PATTERN.matcher(password).find()) {
            violations.add("Password must contain at least one special character (@$!%*?&)");
        }

        return violations;
    }

    /**
     * Checks if a password meets the policy.
     * @param password The password to check
     * @return true if password meets policy, false otherwise
     */
    public boolean isValid(String password) {
        return validateWithViolations(password).isEmpty();
    }
}
