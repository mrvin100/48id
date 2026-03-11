package io.k48.fortyeightid.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.k48.fortyeightid.shared.exception.PasswordPolicyViolationException;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PasswordPolicyServiceTest {

    @InjectMocks
    private PasswordPolicyService passwordPolicyService;

    @Test
    void validate_validPassword_doesNotThrow() {
        // Valid password: 8+ chars, uppercase, lowercase, digit, special char
        passwordPolicyService.validate("Secure@123");
    }

    @Test
    void validate_tooShort_throwsWithViolation() {
        assertThatThrownBy(() -> passwordPolicyService.validate("Aa1@"))
                .isInstanceOf(PasswordPolicyViolationException.class)
                .extracting("violations")
                .asList()
                .anyMatch(v -> v.toString().contains("at least 8 characters"));
    }

    @Test
    void validate_noUppercase_throwsWithViolation() {
        assertThatThrownBy(() -> passwordPolicyService.validate("secure@123"))
                .isInstanceOf(PasswordPolicyViolationException.class)
                .extracting("violations")
                .asList()
                .anyMatch(v -> v.toString().contains("uppercase"));
    }

    @Test
    void validate_noLowercase_throwsWithViolation() {
        assertThatThrownBy(() -> passwordPolicyService.validate("SECURE@123"))
                .isInstanceOf(PasswordPolicyViolationException.class)
                .extracting("violations")
                .asList()
                .anyMatch(v -> v.toString().contains("lowercase"));
    }

    @Test
    void validate_noDigit_throwsWithViolation() {
        assertThatThrownBy(() -> passwordPolicyService.validate("Secure@abc"))
                .isInstanceOf(PasswordPolicyViolationException.class)
                .extracting("violations")
                .asList()
                .anyMatch(v -> v.toString().contains("digit"));
    }

    @Test
    void validate_noSpecialChar_throwsWithViolation() {
        assertThatThrownBy(() -> passwordPolicyService.validate("Secure123"))
                .isInstanceOf(PasswordPolicyViolationException.class)
                .extracting("violations")
                .asList()
                .anyMatch(v -> v.toString().contains("special character"));
    }

    @Test
    void validate_multipleViolations_returnsAllViolations() {
        assertThatThrownBy(() -> passwordPolicyService.validate("short"))
                .isInstanceOf(PasswordPolicyViolationException.class)
                .extracting("violations")
                .asList()
                .satisfies(violations -> {
                    assertThat(violations).hasSize(4); // missing uppercase, digit, special char, and length
                });
    }

    @Test
    void validateWithViolations_returnsEmptyListForValidPassword() {
        var violations = passwordPolicyService.validateWithViolations("Secure@123");
        assertThat(violations).isEmpty();
    }

    @Test
    void validateWithViolations_returnsAllViolations() {
        var violations = passwordPolicyService.validateWithViolations("short");
        
        assertThat(violations).hasSize(4);
        assertThat(violations).anyMatch(v -> v.contains("8 characters"));
        assertThat(violations).anyMatch(v -> v.contains("uppercase"));
        assertThat(violations).anyMatch(v -> v.contains("digit"));
        assertThat(violations).anyMatch(v -> v.contains("special character"));
    }

    @Test
    void isValid_returnsTrueForValidPassword() {
        assertThat(passwordPolicyService.isValid("Secure@123")).isTrue();
    }

    @Test
    void isValid_returnsFalseForInvalidPassword() {
        assertThat(passwordPolicyService.isValid("short")).isFalse();
    }

    @Test
    void validate_nullPassword_throwsWithViolation() {
        assertThatThrownBy(() -> passwordPolicyService.validate(null))
                .isInstanceOf(PasswordPolicyViolationException.class)
                .extracting("violations")
                .asList()
                .isNotEmpty();
    }

    @Test
    void validate_exactlyMinLength_withAllRequirements_passes() {
        // Exactly 8 characters with all requirements
        passwordPolicyService.validate("Aa1@bcde");
    }
}
