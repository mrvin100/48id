package io.k48.fortyeightid.shared.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.FORBIDDEN)
public class OperatorOwnershipRequiredException extends RuntimeException {

    public OperatorOwnershipRequiredException(String message) {
        super(message);
    }
}
