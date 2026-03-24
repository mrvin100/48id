package io.k48.fortyeightid.shared.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.UNAUTHORIZED)
public class AccountLockedException extends RuntimeException {
    
    private final long remainingSeconds;
    
    public AccountLockedException(long remainingSeconds) {
        super(String.format("Account temporarily locked due to too many failed attempts. Try again in %d minutes.", 
                remainingSeconds / 60));
        this.remainingSeconds = remainingSeconds;
    }
    
    public long getRemainingSeconds() {
        return remainingSeconds;
    }
}
