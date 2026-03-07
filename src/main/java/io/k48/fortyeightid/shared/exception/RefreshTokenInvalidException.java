package io.k48.fortyeightid.shared.exception;

public class RefreshTokenInvalidException extends RuntimeException {

    public RefreshTokenInvalidException(String message) {
        super(message);
    }
}
