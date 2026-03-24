package io.k48.fortyeightid.audit;

import lombok.Getter;
import lombok.Setter;
import org.springframework.stereotype.Component;
import org.springframework.web.context.annotation.RequestScope;

/**
 * Request-scoped bean that holds audit context information
 * extracted from the HTTP request (IP address, user agent).
 */
@Component
@RequestScope
@Getter
@Setter
public class AuditContext {

    /**
     * Client IP address extracted from X-Forwarded-For or remote address.
     */
    private String ipAddress;

    /**
     * HTTP User-Agent header from the request.
     */
    private String userAgent;
}
