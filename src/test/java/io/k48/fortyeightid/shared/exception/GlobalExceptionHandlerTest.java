package io.k48.fortyeightid.shared.exception;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void handleUserNotFound_returns404() {
        var problem = handler.handleUserNotFound(new UserNotFoundException("User not found: abc"));
        assertThat(problem.getStatus()).isEqualTo(HttpStatus.NOT_FOUND.value());
        assertThat(problem.getTitle()).isEqualTo("User Not Found");
        assertThat(problem.getDetail()).contains("abc");
    }

    @Test
    void handleDuplicateMatricule_returns409() {
        var problem = handler.handleDuplicateMatricule(new DuplicateMatriculeException("K48-001"));
        assertThat(problem.getStatus()).isEqualTo(HttpStatus.CONFLICT.value());
        assertThat(problem.getTitle()).isEqualTo("Duplicate Matricule");
    }

    @Test
    void handleDuplicateEmail_returns409() {
        var problem = handler.handleDuplicateEmail(new DuplicateEmailException("a@b.com"));
        assertThat(problem.getStatus()).isEqualTo(HttpStatus.CONFLICT.value());
        assertThat(problem.getTitle()).isEqualTo("Duplicate Email");
    }

    @Test
    void handleUnexpected_returns500_withGenericMessage() {
        var problem = handler.handleUnexpected(new RuntimeException("secret details"));
        assertThat(problem.getStatus()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR.value());
        assertThat(problem.getDetail()).isEqualTo("An unexpected error occurred");
        assertThat(problem.getDetail()).doesNotContain("secret details");
    }
}
