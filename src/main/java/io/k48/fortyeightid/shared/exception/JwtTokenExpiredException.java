package io.k48.fortyeightid.shared.exception;

public class JwtTokenExpiredException extends RuntimeException {

    public JwtTokenExpiredException(String message) {
        super(message);
    }
}
