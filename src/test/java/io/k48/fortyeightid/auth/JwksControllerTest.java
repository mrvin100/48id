package io.k48.fortyeightid.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import io.k48.fortyeightid.auth.internal.JwtTokenService;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class JwksControllerTest {

    @Mock
    private JwtTokenService jwtTokenService;

    @InjectMocks
    private JwksController jwksController;

    @Test
    void jwks_returnsJwkSetWithCacheControl() {
        var jwkSet = Map.<String, Object>of("keys", List.of(
                Map.of("kty", "RSA", "use", "sig", "alg", "RS256", "kid", "48id-key-1",
                        "n", "modulus", "e", "AQAB")));
        when(jwtTokenService.getJwkSet()).thenReturn(jwkSet);

        var response = jwksController.jwks();

        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getHeaders().getCacheControl()).contains("max-age=3600");
        assertThat(response.getHeaders().getCacheControl()).contains("public");

        var body = response.getBody();
        assertThat(body).containsKey("keys");
        @SuppressWarnings("unchecked")
        var keys = (List<Map<String, Object>>) body.get("keys");
        assertThat(keys).hasSize(1);
        assertThat(keys.get(0)).containsEntry("kty", "RSA");
        assertThat(keys.get(0)).containsEntry("alg", "RS256");
        assertThat(keys.get(0)).containsEntry("kid", "48id-key-1");
    }
}
