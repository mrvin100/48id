package io.k48.fortyeightid.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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
}
