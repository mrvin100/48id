package io.k48.fortyeightid.shared.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.NOT_FOUND)
public class ApiKeyNotFoundException extends RuntimeException {
    
    public ApiKeyNotFoundException(String message) {
        super(message);
    }
}
