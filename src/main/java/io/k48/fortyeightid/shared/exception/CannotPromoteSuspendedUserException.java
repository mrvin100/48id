package io.k48.fortyeightid.shared.exception;

public class CannotPromoteSuspendedUserException extends RuntimeException {
    public CannotPromoteSuspendedUserException(String message) {
        super(message);
    }
}
