package io.k48.fortyeightid.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import io.k48.fortyeightid.audit.AuditService;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class ApiKeyAuthFilterTest {

    @Mock private ApiKeyManagementPort apiKeyService;
    @Mock private AuditService auditService;
    @InjectMocks private ApiKeyAuthFilter apiKeyAuthFilter;

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void validApiKey_setsAuthenticationAndEmitsAuditEvent() throws Exception {
        ReflectionTestUtils.setField(apiKeyAuthFilter, "apiPrefix", "/api/v1");

        var keyId = UUID.randomUUID();
        var request = new MockHttpServletRequest("POST", "/api/v1/auth/verify-token");
        request.addHeader("X-API-Key", "valid-key");
        var response = new MockHttpServletResponse();
        var chain = new MockFilterChain();

        var apiKey = ApiKey.builder().id(keyId).appName("TestApp").keyHash("hash").active(true).build();
        when(apiKeyService.validate("valid-key")).thenReturn(Optional.of(apiKey));

        apiKeyAuthFilter.doFilterInternal(request, response, chain);

        var auth = SecurityContextHolder.getContext().getAuthentication();
        assertThat(auth).isInstanceOf(ApiKeyAuthentication.class);
        assertThat(auth.getPrincipal()).isEqualTo("TestApp");
        verify(apiKeyService).updateLastUsed(apiKey);
        verify(auditService).log(
                isNull(),
                eq("API_KEY_USED"),
                eq(Map.of("appName", "TestApp", "keyId", keyId.toString())));
    }

    @Test
    void missingHeader_returnsForbidden_noAuditEvent() throws Exception {
        ReflectionTestUtils.setField(apiKeyAuthFilter, "apiPrefix", "/api/v1");

        var request = new MockHttpServletRequest("POST", "/api/v1/auth/verify-token");
        var response = new MockHttpServletResponse();
        var chain = new MockFilterChain();

        apiKeyAuthFilter.doFilterInternal(request, response, chain);

        assertThat(response.getStatus()).isEqualTo(403);
        verifyNoInteractions(auditService);
    }

    @Test
    void invalidKey_returnsForbidden_noAuditEvent() throws Exception {
        ReflectionTestUtils.setField(apiKeyAuthFilter, "apiPrefix", "/api/v1");

        var request = new MockHttpServletRequest("GET", "/api/v1/users/abc/exists");
        request.addHeader("X-API-Key", "bad-key");
        var response = new MockHttpServletResponse();
        var chain = new MockFilterChain();

        when(apiKeyService.validate("bad-key")).thenReturn(Optional.empty());

        apiKeyAuthFilter.doFilterInternal(request, response, chain);

        assertThat(response.getStatus()).isEqualTo(403);
        verifyNoInteractions(auditService);
    }

    @Test
    void shouldNotFilterPublicEndpoint() {
        ReflectionTestUtils.setField(apiKeyAuthFilter, "apiPrefix", "/api/v1");
        var request = new MockHttpServletRequest("POST", "/api/v1/auth/login");
        assertThat(apiKeyAuthFilter.shouldNotFilter(request)).isTrue();
    }
}
