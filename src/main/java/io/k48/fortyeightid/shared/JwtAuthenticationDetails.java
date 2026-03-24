package io.k48.fortyeightid.shared;

import org.springframework.security.web.authentication.WebAuthenticationDetails;

/**
 * Extends WebAuthenticationDetails to carry the JWT matricule claim,
 * allowing cross-module access without importing identity internals.
 */
public class JwtAuthenticationDetails extends WebAuthenticationDetails {

    private final String matricule;

    public JwtAuthenticationDetails(WebAuthenticationDetails base, String matricule) {
        super(base.getRemoteAddress(), base.getSessionId());
        this.matricule = matricule;
    }

    /** Test-friendly constructor. */
    public JwtAuthenticationDetails(String remoteAddress, String sessionId, String matricule) {
        super(remoteAddress, sessionId);
        this.matricule = matricule;
    }

    public String getMatricule() {
        return matricule;
    }
}
