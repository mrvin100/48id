package io.k48.fortyeightid.shared.exception;

public class JwtSignatureException extends RuntimeException {

    public JwtSignatureException(String message) {
        super(message);
    }
}
