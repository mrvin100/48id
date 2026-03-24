package io.k48.fortyeightid.auth;

/**
 * Port for JWT token validation operations.
 * This interface defines the contract for validating JWT tokens
 * without exposing internal implementation details.
 */
public interface JwtValidationPort {
    
    /**
     * Validates a JWT token and returns a JWT claims object if valid.
     * 
     * @param token the JWT token to validate
     * @return the validated JWT claims
     * @throws RuntimeException if token is invalid, expired, or malformed
     */
    ValidatedJwt validateToken(String token);
    
    /**
     * Represents a validated JWT with access to its claims.
     */
    interface ValidatedJwt {
        /**
         * Gets the subject (sub) claim from the JWT.
         * @return the subject claim
         */
        String getSubject();
        
        /**
         * Gets a claim value from the JWT.
         * @param name the claim name
         * @return the claim value, or null if not present
         */
        Object getClaim(String name);
    }
}