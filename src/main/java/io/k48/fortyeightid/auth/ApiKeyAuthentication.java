package io.k48.fortyeightid.auth;

import java.util.List;
import java.util.UUID;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

public class ApiKeyAuthentication extends AbstractAuthenticationToken {

    private final UUID apiKeyId;
    private final String appName;

    public ApiKeyAuthentication(UUID apiKeyId, String appName) {
        super(List.of(new SimpleGrantedAuthority("ROLE_API_CLIENT")));
        this.apiKeyId = apiKeyId;
        this.appName = appName;
        setAuthenticated(true);
    }

    @Override
    public Object getCredentials() {
        return null;
    }

    @Override
    public Object getPrincipal() {
        return appName;
    }

    public UUID getApiKeyId() {
        return apiKeyId;
    }
}
