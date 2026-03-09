package io.k48.fortyeightid.shared.config;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.BadCredentialsException;

class ProblemDetailAuthenticationEntryPointTest {

    private final ObjectMapper objectMapper = Jackson2ObjectMapperBuilder.json().build();
    private final ProblemDetailAuthenticationEntryPoint entryPoint =
            new ProblemDetailAuthenticationEntryPoint(objectMapper);

    @Test
    void returns401WithProblemDetailJson() throws Exception {
        var request = new MockHttpServletRequest();
        var response = new MockHttpServletResponse();

        entryPoint.commence(request, response, new BadCredentialsException("test"));

        assertThat(response.getStatus()).isEqualTo(401);
        assertThat(response.getContentType()).isEqualTo("application/problem+json");
        assertThat(response.getContentAsString()).contains("UNAUTHORIZED");
        assertThat(response.getContentAsString()).contains("https://48id.k48.io/errors/unauthorized");
    }
}
