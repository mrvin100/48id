package io.k48.fortyeightid.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
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
    void validApiKeySetsAuthenticationForProtectedEndpoint() throws Exception {
        ReflectionTestUtils.setField(apiKeyAuthFilter, "apiPrefix", "/api/v1");

        var request = new MockHttpServletRequest("POST", "/api/v1/auth/verify-token");
        request.addHeader("X-API-Key", "valid-key");
        var response = new MockHttpServletResponse();
        var chain = new MockFilterChain();

        var apiKey = ApiKey.builder()
                .id(UUID.randomUUID())
                .appName("TestApp")
                .keyHash("hash")
                .active(true)
                .build();
        when(apiKeyService.validate("valid-key")).thenReturn(Optional.of(apiKey));

        apiKeyAuthFilter.doFilterInternal(request, response, chain);

        var auth = SecurityContextHolder.getContext().getAuthentication();
        assertThat(auth).isInstanceOf(ApiKeyAuthentication.class);
        assertThat(auth.getPrincipal()).isEqualTo("TestApp");
        assertThat(auth.getAuthorities()).anyMatch(a -> a.getAuthority().equals("ROLE_API_CLIENT"));
        verify(apiKeyService).updateLastUsed(apiKey);
    }

    @Test
    void missingHeaderReturnsForbiddenForProtectedEndpoint() throws Exception {
        ReflectionTestUtils.setField(apiKeyAuthFilter, "apiPrefix", "/api/v1");

        var request = new MockHttpServletRequest("POST", "/api/v1/auth/verify-token");
        var response = new MockHttpServletResponse();
        var chain = new MockFilterChain();

        apiKeyAuthFilter.doFilterInternal(request, response, chain);

        assertThat(response.getStatus()).isEqualTo(403);
        assertThat(response.getContentAsString()).contains("Missing X-API-Key header");
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    @Test
    void invalidKeyReturnsForbiddenForProtectedEndpoint() throws Exception {
        ReflectionTestUtils.setField(apiKeyAuthFilter, "apiPrefix", "/api/v1");

        var request = new MockHttpServletRequest("GET", "/api/v1/users/abc/exists");
        request.addHeader("X-API-Key", "bad-key");
        var response = new MockHttpServletResponse();
        var chain = new MockFilterChain();

        when(apiKeyService.validate("bad-key")).thenReturn(Optional.empty());

        apiKeyAuthFilter.doFilterInternal(request, response, chain);

        assertThat(response.getStatus()).isEqualTo(403);
        assertThat(response.getContentAsString()).contains("Invalid X-API-Key header");
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    @Test
    void shouldNotFilterPublicEndpoint() {
        ReflectionTestUtils.setField(apiKeyAuthFilter, "apiPrefix", "/api/v1");
        var request = new MockHttpServletRequest("POST", "/api/v1/auth/login");
        assertThat(apiKeyAuthFilter.shouldNotFilter(request)).isTrue();
    }

    // --- WEB-S4-BE-03: API_KEY_USED audit event ---

    @Test
    void doFilterInternal_shouldEmitApiKeyUsedAuditEvent_whenValidKey() throws Exception {
        ReflectionTestUtils.setField(apiKeyAuthFilter, "apiPrefix", "/api/v1");

        var keyId = UUID.randomUUID();
        var apiKey = ApiKey.builder().id(keyId).appName("48Hub").keyHash("hash").active(true).build();
        when(apiKeyService.validate("valid-key")).thenReturn(Optional.of(apiKey));

        var request = new MockHttpServletRequest("POST", "/api/v1/auth/verify-token");
        request.addHeader("X-API-Key", "valid-key");

        apiKeyAuthFilter.doFilterInternal(request, new MockHttpServletResponse(), new MockFilterChain());

        verify(auditService).log(
                isNull(),
                eq("API_KEY_USED"),
                eq(Map.of("appName", "48Hub", "keyId", keyId.toString()))
        );
    }

    @Test
    void doFilterInternal_shouldNotEmitAuditEvent_whenInvalidKey() throws Exception {
        ReflectionTestUtils.setField(apiKeyAuthFilter, "apiPrefix", "/api/v1");

        when(apiKeyService.validate("bad-key")).thenReturn(Optional.empty());

        var request = new MockHttpServletRequest("POST", "/api/v1/auth/verify-token");
        request.addHeader("X-API-Key", "bad-key");

        apiKeyAuthFilter.doFilterInternal(request, new MockHttpServletResponse(), new MockFilterChain());

        verify(auditService, never()).log(any(), any(), any());
    }

    @Test
    void doFilterInternal_shouldContinueRequest_whenAuditWriteFails() throws Exception {
        ReflectionTestUtils.setField(apiKeyAuthFilter, "apiPrefix", "/api/v1");

        var keyId = UUID.randomUUID();
        var apiKey = ApiKey.builder().id(keyId).appName("48Hub").keyHash("hash").active(true).build();
        when(apiKeyService.validate("valid-key")).thenReturn(Optional.of(apiKey));
        doThrow(new RuntimeException("DB down")).when(auditService).log(any(), any(), any());

        var request = new MockHttpServletRequest("POST", "/api/v1/auth/verify-token");
        request.addHeader("X-API-Key", "valid-key");
        var response = new MockHttpServletResponse();
        var chain = new MockFilterChain();

        // Must not throw — request continues normally
        apiKeyAuthFilter.doFilterInternal(request, response, chain);

        assertThat(response.getStatus()).isEqualTo(200);
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNotNull();
    }

    @Test
    void doFilterInternal_shouldEmitAuditAfterUpdateLastUsed_whenValidKey() throws Exception {
        ReflectionTestUtils.setField(apiKeyAuthFilter, "apiPrefix", "/api/v1");

        var keyId = UUID.randomUUID();
        var apiKey = ApiKey.builder().id(keyId).appName("48Hub").keyHash("hash").active(true).build();
        when(apiKeyService.validate("valid-key")).thenReturn(Optional.of(apiKey));

        var order = org.mockito.Mockito.inOrder(apiKeyService, auditService);

        var request = new MockHttpServletRequest("POST", "/api/v1/auth/verify-token");
        request.addHeader("X-API-Key", "valid-key");

        apiKeyAuthFilter.doFilterInternal(request, new MockHttpServletResponse(), new MockFilterChain());

        order.verify(apiKeyService).updateLastUsed(apiKey);
        order.verify(auditService).log(isNull(), eq("API_KEY_USED"), any());
    }
}
