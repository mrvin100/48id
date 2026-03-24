package io.k48.fortyeightid.shared.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.BAD_REQUEST)
public class NewPasswordSameAsCurrentException extends RuntimeException {
    
    public NewPasswordSameAsCurrentException(String message) {
        super(message);
    }
}
