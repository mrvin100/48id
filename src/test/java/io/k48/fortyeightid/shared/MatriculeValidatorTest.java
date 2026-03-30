package io.k48.fortyeightid.shared;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class MatriculeValidatorTest {

    @Test
    void validate_validMatriculeAndMatchingBatch_returnsEmpty() {
        assertThat(MatriculeValidator.validate("K48-B1-12", "B1")).isEmpty();
    }

    @Test
    void validate_validMatriculeNoBatch_returnsEmpty() {
        assertThat(MatriculeValidator.validate("K48-B2-5", null)).isEmpty();
    }

    @Test
    void validate_invalidFormat_returnsError() {
        var result = MatriculeValidator.validate("K48-2024-001", "B1");
        assertThat(result).isPresent();
        assertThat(result.get()).contains("does not match required format K48-B{n}-{seq}");
    }

    @Test
    void validate_prefixMismatch_returnsExactMessage() {
        var result = MatriculeValidator.validate("K48-B2-5", "B1");
        assertThat(result).isPresent();
        assertThat(result.get()).isEqualTo("Matricule prefix 'K48-B2' does not match batch 'B1'");
    }

    @Test
    void validate_nullMatricule_returnsError() {
        assertThat(MatriculeValidator.validate(null, "B1")).isPresent();
    }
}
