package io.k48.fortyeightid.auth;

import io.k48.fortyeightid.auth.internal.JwtTokenService;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import io.k48.fortyeightid.shared.exception.JwtSignatureException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletResponse;
import java.time.Instant;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;

class JwtAuthenticationFilterTest {

    private JwtTokenService jwtTokenService;
    private JwtAuthenticationFilter filter;

    @BeforeEach
    void setUp() {
        jwtTokenService = mock(JwtTokenService.class);
        filter = new JwtAuthenticationFilter(jwtTokenService);
        SecurityContextHolder.clearContext();
    }

    @Test
    void setsAuthenticationOnValidToken() throws Exception {
        var request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer valid-token");
        var response = mock(HttpServletResponse.class);
        var chain = mock(FilterChain.class);

        var jwt = new Jwt("valid-token", Instant.now(), Instant.now().plusSeconds(900),
                Map.of("alg", "RS256"), Map.of("sub", "user-id", "role", "ROLE_STUDENT"));
        when(jwtTokenService.validateToken("valid-token")).thenReturn(jwt);

        filter.doFilterInternal(request, response, chain);

        var auth = SecurityContextHolder.getContext().getAuthentication();
        assertThat(auth).isNotNull();
        assertThat(auth.getName()).isEqualTo("user-id");
        assertThat(auth.getAuthorities()).hasSize(1);
    }

    @Test
    void skipsAuthenticationOnMissingHeader() throws Exception {
        var request = new MockHttpServletRequest();
        var response = mock(HttpServletResponse.class);
        var chain = mock(FilterChain.class);

        filter.doFilterInternal(request, response, chain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    @Test
    void skipsAuthenticationOnInvalidToken() throws Exception {
        var request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer bad-token");
        var response = mock(HttpServletResponse.class);
        var chain = mock(FilterChain.class);

        when(jwtTokenService.validateToken("bad-token")).thenThrow(new JwtSignatureException("invalid"));

        filter.doFilterInternal(request, response, chain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }
}
