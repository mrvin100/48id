package io.k48.fortyeightid.auth;

import static org.assertj.core.api.Assertions.assertThat;
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

@ExtendWith(MockitoExtension.class)
class ApiKeyAuthFilterTest {

    @Mock private ApiKeyManagementPort apiKeyService;
    @InjectMocks private ApiKeyAuthFilter apiKeyAuthFilter;

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void validApiKeySetsAuthentication() throws Exception {
        var request = new MockHttpServletRequest();
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
    }

    @Test
    void missingHeaderSkipsFilter() throws Exception {
        var request = new MockHttpServletRequest();
        var response = new MockHttpServletResponse();
        var chain = new MockFilterChain();

        apiKeyAuthFilter.doFilterInternal(request, response, chain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    @Test
    void invalidKeyDoesNotSetAuthentication() throws Exception {
        var request = new MockHttpServletRequest();
        request.addHeader("X-API-Key", "bad-key");
        var response = new MockHttpServletResponse();
        var chain = new MockFilterChain();

        when(apiKeyService.validate("bad-key")).thenReturn(Optional.empty());

        apiKeyAuthFilter.doFilterInternal(request, response, chain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }
}
