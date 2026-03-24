package io.k48.fortyeightid.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import io.k48.fortyeightid.shared.exception.JwtSignatureException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.core.context.SecurityContextHolder;

class JwtAuthenticationFilterTest {

    private JwtValidationPort jwtValidationPort;
    private JwtAuthenticationFilter filter;

    @BeforeEach
    void setUp() {
        jwtValidationPort = mock(JwtValidationPort.class);
        filter = new JwtAuthenticationFilter(jwtValidationPort);
        SecurityContextHolder.clearContext();
    }

    @Test
    void setsAuthenticationOnValidToken() throws Exception {
        var request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer valid-token");
        var response = mock(HttpServletResponse.class);
        var chain = mock(FilterChain.class);

        var jwt = mock(JwtValidationPort.ValidatedJwt.class);
        when(jwt.getSubject()).thenReturn("user-id");
        when(jwt.getClaim("role")).thenReturn("ROLE_STUDENT");
        when(jwtValidationPort.validateToken("valid-token")).thenReturn(jwt);

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

        when(jwtValidationPort.validateToken("bad-token")).thenThrow(new JwtSignatureException("invalid"));

        filter.doFilterInternal(request, response, chain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }
}
