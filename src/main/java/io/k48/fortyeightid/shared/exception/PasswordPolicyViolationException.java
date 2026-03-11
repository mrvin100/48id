package io.k48.fortyeightid.shared.exception;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.BAD_REQUEST)
public class PasswordPolicyViolationException extends RuntimeException {
    
    private final List<String> violations;
    
    public PasswordPolicyViolationException(List<String> violations) {
        super("Password does not meet policy requirements");
        this.violations = violations;
    }
    
    @JsonProperty("violations")
    public List<String> getViolations() {
        return violations;
    }
}
