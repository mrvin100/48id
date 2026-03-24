package io.k48.fortyeightid.shared.config;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.security.access.prepost.PreAuthorize;

/**
 * Verifies that security annotations are correctly applied to controllers.
 * These are compile-time / reflection checks — no Spring context needed.
 */
class SecurityAnnotationTest {

    @Test
    void adminOperatorAccountController_requiresAdminRole() throws Exception {
        var annotation = Class.forName("io.k48.fortyeightid.operator.internal.AdminOperatorAccountController")
                .getAnnotation(PreAuthorize.class);
        assertThat(annotation).isNotNull();
        assertThat(annotation.value()).isEqualTo("hasRole('ADMIN')");
    }

    @Test
    void operatorApiKeyController_requiresOperatorRole() throws Exception {
        var annotation = Class.forName("io.k48.fortyeightid.operator.internal.OperatorApiKeyController")
                .getAnnotation(PreAuthorize.class);
        assertThat(annotation).isNotNull();
        assertThat(annotation.value()).isEqualTo("hasRole('OPERATOR')");
    }
}
