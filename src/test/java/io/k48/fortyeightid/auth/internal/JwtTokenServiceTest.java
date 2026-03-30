package io.k48.fortyeightid.auth.internal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.k48.fortyeightid.identity.User;
import io.k48.fortyeightid.identity.UserStatus;
import io.k48.fortyeightid.shared.exception.JwtSignatureException;
import io.k48.fortyeightid.shared.exception.JwtTokenExpiredException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.util.UUID;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class JwtTokenServiceTest {

    private static JwtTokenService jwtTokenService;
    private static JwtTokenService expiredTokenService;

    @BeforeAll
    static void setUp() throws Exception {
        var keyPair = generateKeyPair();

        var config = new JwtConfig();
        config.setIssuer("http://localhost:8080");
        config.setAccessTokenExpiry(900);
        config.setRefreshTokenExpiry(86400);
        config.setRsaPublicKey((RSAPublicKey) keyPair.getPublic());
        config.setRsaPrivateKey((RSAPrivateKey) keyPair.getPrivate());
        jwtTokenService = new JwtTokenService(config);

        var shortLivedConfig = new JwtConfig();
        shortLivedConfig.setIssuer("http://localhost:8080");
        shortLivedConfig.setAccessTokenExpiry(1); // 1 second
        shortLivedConfig.setRefreshTokenExpiry(86400);
        shortLivedConfig.setRsaPublicKey((RSAPublicKey) keyPair.getPublic());
        shortLivedConfig.setRsaPrivateKey((RSAPrivateKey) keyPair.getPrivate());
        expiredTokenService = new JwtTokenService(shortLivedConfig);
    }

    private static KeyPair generateKeyPair() throws Exception {
        var gen = KeyPairGenerator.getInstance("RSA");
        gen.initialize(2048);
        return gen.generateKeyPair();
    }

    private UserPrincipal createPrincipal() {
        var user = User.builder()
                .id(UUID.randomUUID())
                .matricule("K48-B1-1")
                .email("test@k48.io")
                .name("Test User")
                .passwordHash("$2a$10$hash")
                .status(UserStatus.ACTIVE)
                .batch("2024")
                .build();
        return new UserPrincipal(user);
    }

    private User createUser() {
        return User.builder()
                .id(UUID.randomUUID())
                .matricule("K48-B1-1")
                .email("test@k48.io")
                .name("Test User")
                .passwordHash("$2a$10$hash")
                .status(UserStatus.ACTIVE)
                .batch("2024")
                .build();
    }

    @Test
    void generateAccessToken_returnsValidJwt() {
        var principal = createPrincipal();
        var user = createUser();
        var token = jwtTokenService.generateAccessToken(principal, user);

        assertThat(token).isNotBlank();

        var jwt = jwtTokenService.validateTokenInternal(token);
        assertThat(jwt.getSubject()).isEqualTo(user.getId().toString());
        assertThat((String) jwt.getClaim("matricule")).isEqualTo("K48-B1-1");
        assertThat((String) jwt.getClaim("name")).isEqualTo("Test User");
        assertThat((String) jwt.getClaim("batch")).isEqualTo("2024");
        assertThat(jwt.getIssuer().toString()).isEqualTo("http://localhost:8080");
    }

    @Test
    void validateToken_throwsOnExpiredToken() throws InterruptedException {
        var principal = createPrincipal();
        var user = createUser();
        var token = expiredTokenService.generateAccessToken(principal, user);

        Thread.sleep(2000); // wait for 1-second token to expire

        assertThatThrownBy(() -> jwtTokenService.validateToken(token))
                .isInstanceOf(JwtTokenExpiredException.class);
    }

    @Test
    void validateToken_throwsOnTamperedToken() {
        var principal = createPrincipal();
        var user = createUser();
        var token = jwtTokenService.generateAccessToken(principal, user);
        var tampered = token.substring(0, token.length() - 5) + "XXXXX";

        assertThatThrownBy(() -> jwtTokenService.validateToken(tampered))
                .isInstanceOf(JwtSignatureException.class);
    }

    @Test
    void getClaims_returnsClaims() {
        var principal = createPrincipal();
        var user = createUser();
        var token = jwtTokenService.generateAccessToken(principal, user);

        var claims = jwtTokenService.getClaims(token);
        assertThat(claims).containsKey("sub");
        assertThat(claims).containsKey("matricule");
        assertThat(claims).containsKey("role");
    }
}
