package io.k48.fortyeightid.shared.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.NOT_FOUND)
public class OperatorAccountNotFoundException extends RuntimeException {

    public OperatorAccountNotFoundException(String message) {
        super(message);
    }
}
