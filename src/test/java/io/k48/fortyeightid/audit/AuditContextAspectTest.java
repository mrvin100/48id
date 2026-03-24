package io.k48.fortyeightid.audit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.k48.fortyeightid.shared.JwtAuthenticationDetails;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.aspectj.lang.ProceedingJoinPoint;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@ExtendWith(MockitoExtension.class)
class AuditContextAspectTest {

    @Mock private AuditService auditService;
    @Mock private AuditContext auditContext;
    @InjectMocks private AuditContextAspect aspect;

    private MockHttpServletRequest request;
    private ProceedingJoinPoint joinPoint;

    @BeforeEach
    void setUp() throws Throwable {
        request = new MockHttpServletRequest("GET", "/api/v1/operator/users");
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));
        joinPoint = mock(ProceedingJoinPoint.class);
        when(joinPoint.proceed()).thenReturn(null);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
        RequestContextHolder.resetRequestAttributes();
    }

    @Test
    void operatorPrincipal_emitsOperatorActionWithDetails() throws Throwable {
        var userId = UUID.randomUUID();
        setOperatorAuthentication(userId, "K48-B1-3");

        aspect.captureOperatorAction(joinPoint);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<Map<String, Object>> captor = ArgumentCaptor.forClass(Map.class);
        verify(auditService).log(eq(userId), eq("OPERATOR_ACTION"), captor.capture());

        var details = captor.getValue();
        assertThat(details).containsEntry("endpoint", "/api/v1/operator/users");
        assertThat(details).containsEntry("method", "GET");
        assertThat(details).containsEntry("userId", userId.toString());
        assertThat(details).containsEntry("matricule", "K48-B1-3");
    }

    @Test
    void adminPrincipal_doesNotEmitOperatorAction() throws Throwable {
        var auth = new UsernamePasswordAuthenticationToken(
                UUID.randomUUID().toString(), null,
                List.of(new SimpleGrantedAuthority("ROLE_ADMIN")));
        SecurityContextHolder.getContext().setAuthentication(auth);

        aspect.captureOperatorAction(joinPoint);

        verify(auditService, never()).log(any(), eq("OPERATOR_ACTION"), any());
    }

    @Test
    void studentPrincipal_doesNotEmitOperatorAction() throws Throwable {
        var auth = new UsernamePasswordAuthenticationToken(
                UUID.randomUUID().toString(), null,
                List.of(new SimpleGrantedAuthority("ROLE_STUDENT")));
        SecurityContextHolder.getContext().setAuthentication(auth);

        aspect.captureOperatorAction(joinPoint);

        verify(auditService, never()).log(any(), eq("OPERATOR_ACTION"), any());
    }

    @Test
    void noAuthentication_doesNotEmitOperatorAction() throws Throwable {
        aspect.captureOperatorAction(joinPoint);
        verify(auditService, never()).log(any(), eq("OPERATOR_ACTION"), any());
    }

    @Test
    void operatorPrincipal_withoutMatriculeInDetails_usesUnknown() throws Throwable {
        var userId = UUID.randomUUID();
        // Plain auth without JwtAuthenticationDetails
        var auth = new UsernamePasswordAuthenticationToken(
                userId.toString(), null,
                List.of(new SimpleGrantedAuthority("ROLE_OPERATOR")));
        SecurityContextHolder.getContext().setAuthentication(auth);

        aspect.captureOperatorAction(joinPoint);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<Map<String, Object>> captor = ArgumentCaptor.forClass(Map.class);
        verify(auditService).log(eq(userId), eq("OPERATOR_ACTION"), captor.capture());
        assertThat(captor.getValue()).containsEntry("matricule", "unknown");
    }

    // ── Helper ────────────────────────────────────────────────────────────────

    private void setOperatorAuthentication(UUID userId, String matricule) {
        var auth = new UsernamePasswordAuthenticationToken(
                userId.toString(), null,
                List.of(new SimpleGrantedAuthority("ROLE_OPERATOR")));
        auth.setDetails(new JwtAuthenticationDetails("127.0.0.1", null, matricule));
        SecurityContextHolder.getContext().setAuthentication(auth);
    }
}
