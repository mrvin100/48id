package io.k48.fortyeightid.auth.internal;

import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "fortyeightid.jwt")
@Getter
@Setter
class JwtConfig {

    private String issuer;
    private long accessTokenExpiry;
    private long refreshTokenExpiry;
    private RSAPublicKey rsaPublicKey;
    private RSAPrivateKey rsaPrivateKey;
}
