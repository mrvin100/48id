package io.k48.fortyeightid.auth.internal;

import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.source.ImmutableJWKSet;
import io.k48.fortyeightid.identity.User;
import io.k48.fortyeightid.shared.exception.JwtSignatureException;
import io.k48.fortyeightid.shared.exception.JwtTokenExpiredException;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.security.oauth2.jwt.JwtTimestampValidator;
import org.springframework.security.oauth2.jwt.JwtValidationException;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class JwtTokenService {

    private final JwtConfig jwtConfig;
    private final JwtEncoder jwtEncoder;
    private final JwtDecoder jwtDecoder;

    JwtTokenService(JwtConfig jwtConfig) {
        this.jwtConfig = jwtConfig;

        var rsaKey = new RSAKey.Builder(jwtConfig.getRsaPublicKey())
                .privateKey(jwtConfig.getRsaPrivateKey())
                .build();
        this.jwtEncoder = new NimbusJwtEncoder(new ImmutableJWKSet<>(new JWKSet(rsaKey)));
        var decoder = NimbusJwtDecoder.withPublicKey(jwtConfig.getRsaPublicKey()).build();
        decoder.setJwtValidator(new JwtTimestampValidator(Duration.ZERO));
        this.jwtDecoder = decoder;
    }

    String generateAccessToken(UserPrincipal principal, User user) {
        var now = Instant.now();
        var roles = principal.getAuthorities().stream()
                .map(Object::toString)
                .collect(Collectors.joining(","));

        var claims = JwtClaimsSet.builder()
                .issuer(jwtConfig.getIssuer())
                .subject(user.getId().toString())
                .issuedAt(now)
                .expiresAt(now.plusSeconds(jwtConfig.getAccessTokenExpiry()))
                .claim("matricule", user.getMatricule())
                .claim("name", user.getName())
                .claim("role", roles)
                .claim("batch", user.getBatch() != null ? user.getBatch() : "")
                .build();

        var header = JwsHeader.with(SignatureAlgorithm.RS256).build();
        return jwtEncoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
    }

    public Jwt validateToken(String token) {
        try {
            return jwtDecoder.decode(token);
        } catch (JwtValidationException ex) {
            if (ex.getErrors().stream().anyMatch(e ->
                    e.getDescription().contains("expired"))) {
                throw new JwtTokenExpiredException("Token has expired");
            }
            throw new JwtSignatureException("Token validation failed: " + ex.getMessage());
        } catch (JwtException ex) {
            throw new JwtSignatureException("Token is invalid: " + ex.getMessage());
        }
    }

    public Map<String, Object> getJwkSet() {
        var rsaKey = new RSAKey.Builder(jwtConfig.getRsaPublicKey())
                .keyUse(com.nimbusds.jose.jwk.KeyUse.SIGNATURE)
                .algorithm(com.nimbusds.jose.JWSAlgorithm.RS256)
                .keyID("48id-key-1")
                .build();
        return new JWKSet(rsaKey).toJSONObject();
    }

    Map<String, Object> getClaims(String token) {
        return validateToken(token).getClaims();
    }
}
