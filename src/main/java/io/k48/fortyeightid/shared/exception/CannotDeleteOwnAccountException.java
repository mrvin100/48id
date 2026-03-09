package io.k48.fortyeightid.shared.exception;

public class CannotDeleteOwnAccountException extends RuntimeException {
    public CannotDeleteOwnAccountException(String message) {
        super(message);
    }
}
