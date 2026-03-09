package io.k48.fortyeightid.shared.exception;

public class CannotChangeOwnRoleException extends RuntimeException {
    public CannotChangeOwnRoleException(String message) {
        super(message);
    }
}
