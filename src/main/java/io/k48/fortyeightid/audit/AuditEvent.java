package io.k48.fortyeightid.audit;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation for methods that should automatically generate audit log entries.
 * When applied to a method, the AuditContextAspect will automatically log
 * the specified event type upon successful method execution.
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface AuditEvent {
    /**
     * The type of audit event to log.
     * Examples: LOGIN_SUCCESS, PASSWORD_CHANGED, ACCOUNT_SUSPENDED
     */
    String type();

    /**
     * The SpEL expression to extract the user ID from method parameters or result.
     * Default: "#userId" extracts the parameter named userId
     */
    String userIdExpression() default "#userId";
}
