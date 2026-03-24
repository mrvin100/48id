package io.k48.fortyeightid.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import io.k48.fortyeightid.audit.AuditService;
import io.k48.fortyeightid.auth.internal.UserPrincipal;
import io.k48.fortyeightid.identity.User;
import io.k48.fortyeightid.identity.UserStatus;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

@ExtendWith(MockitoExtension.class)
class OperatorActionAuditFilterTest {

    @Mock private AuditService auditService;

    private OperatorActionAuditFilter filter;

    @BeforeEach
    void setUp() {
        filter = new OperatorActionAuditFilter(auditService, "/api/v1");
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    // --- helpers ---

    private UserPrincipal operatorPrincipal(UUID id, String matricule) {
        var role = new io.k48.fortyeightid.identity.Role();
        role.setName("OPERATOR");
        var user = User.builder()
                .id(id)
                .matricule(matricule)
                .passwordHash("hash")
                .status(UserStatus.ACTIVE)
                .roles(Set.of(role))
                .build();
        return new UserPrincipal(user);
    }

    private void setAuthentication(UserPrincipal principal, String... roles) {
        var authorities = java.util.Arrays.stream(roles)
                .map(SimpleGrantedAuthority::new)
                .toList();
        var auth = new UsernamePasswordAuthenticationToken(principal, null, authorities);
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    // --- acceptance criteria ---

    @Test
    void doFilterInternal_shouldEmitOperatorAction_whenOperatorCallsOperatorEndpoint() throws Exception {
        var userId = UUID.randomUUID();
        var principal = operatorPrincipal(userId, "K48-B1-7");
        setAuthentication(principal, "ROLE_OPERATOR");

        var request = new MockHttpServletRequest("GET", "/api/v1/operator/users");
        var response = new MockHttpServletResponse();
        var chain = new MockFilterChain();

        filter.doFilterInternal(request, response, chain);

        verify(auditService).log(
                eq(userId),
                eq("OPERATOR_ACTION"),
                eq(Map.of("endpoint", "/api/v1/operator/users", "method", "GET", "matricule", "K48-B1-7"))
        );
        assertThat(response.getStatus()).isEqualTo(200);
    }

    @Test
    void doFilterInternal_shouldNotEmitAuditEvent_whenOperatorCallsNonOperatorEndpoint() throws Exception {
        var userId = UUID.randomUUID();
        var principal = operatorPrincipal(userId, "K48-B1-7");
        setAuthentication(principal, "ROLE_OPERATOR");

        var request = new MockHttpServletRequest("POST", "/api/v1/auth/refresh");

        filter.doFilterInternal(request, new MockHttpServletResponse(), new MockFilterChain());

        verify(auditService, never()).log(any(), any(), any());
    }

    @Test
    void doFilterInternal_shouldNotEmitAuditEvent_whenAdminCallsOperatorEndpoint() throws Exception {
        var adminPrincipal = operatorPrincipal(UUID.randomUUID(), "K48-B1-1");
        setAuthentication(adminPrincipal, "ROLE_ADMIN");

        var request = new MockHttpServletRequest("GET", "/api/v1/operator/users");

        filter.doFilterInternal(request, new MockHttpServletResponse(), new MockFilterChain());

        verify(auditService, never()).log(any(), any(), any());
    }

    @Test
    void doFilterInternal_shouldNotEmitAuditEvent_whenStudentCallsAnyEndpoint() throws Exception {
        var studentPrincipal = operatorPrincipal(UUID.randomUUID(), "K48-B1-5");
        setAuthentication(studentPrincipal, "ROLE_STUDENT");

        var request = new MockHttpServletRequest("GET", "/api/v1/operator/users");

        filter.doFilterInternal(request, new MockHttpServletResponse(), new MockFilterChain());

        verify(auditService, never()).log(any(), any(), any());
    }

    @Test
    void doFilterInternal_shouldNotEmitAuditEvent_whenNoAuthentication() throws Exception {
        var request = new MockHttpServletRequest("GET", "/api/v1/operator/users");

        filter.doFilterInternal(request, new MockHttpServletResponse(), new MockFilterChain());

        verify(auditService, never()).log(any(), any(), any());
    }

    @Test
    void doFilterInternal_shouldContinueRequest_whenAuditWriteFails() throws Exception {
        var userId = UUID.randomUUID();
        var principal = operatorPrincipal(userId, "K48-B1-7");
        setAuthentication(principal, "ROLE_OPERATOR");

        doThrow(new RuntimeException("DB down")).when(auditService).log(any(), any(), any());

        var request = new MockHttpServletRequest("POST", "/api/v1/operator/api-keys");
        var response = new MockHttpServletResponse();
        var chain = new MockFilterChain();

        filter.doFilterInternal(request, response, chain);

        assertThat(response.getStatus()).isEqualTo(200);
    }

    @Test
    void doFilterInternal_shouldIncludeExactJsonbFieldNames_whenEmittingEvent() throws Exception {
        var userId = UUID.randomUUID();
        var principal = operatorPrincipal(userId, "K48-B1-3");
        setAuthentication(principal, "ROLE_OPERATOR");

        var request = new MockHttpServletRequest("POST", "/api/v1/operator/api-keys");

        filter.doFilterInternal(request, new MockHttpServletResponse(), new MockFilterChain());

        verify(auditService).log(
                eq(userId),
                eq("OPERATOR_ACTION"),
                eq(Map.of("endpoint", "/api/v1/operator/api-keys", "method", "POST", "matricule", "K48-B1-3"))
        );
    }
}
