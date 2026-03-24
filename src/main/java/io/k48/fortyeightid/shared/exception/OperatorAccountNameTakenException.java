package io.k48.fortyeightid.shared.exception;

public class OperatorAccountNameTakenException extends RuntimeException {

    public OperatorAccountNameTakenException(String name) {
        super("Operator account name already taken: " + name);
    }
}
