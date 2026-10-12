package com.satyam.splitly.common.exception;

import jakarta.servlet.http.HttpServletRequest;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;
import tools.jackson.databind.exc.InvalidFormatException;

/**
 * Turns exceptions into the one {@link ErrorResponse} shape, for every controller
 * at once.
 *
 * <p>This is why no controller in this project contains a try/catch. A service
 * throws {@link NotFoundException}, the throw unwinds to here, and this class
 * decides the status code and body. Controllers stay about the happy path.
 *
 * <p>Order matters: Spring picks the handler whose parameter type is the closest
 * match, so the specific handlers below always win over the {@code Exception}
 * catch-all at the bottom. Anything Spring MVC itself throws needs a handler here,
 * or the catch-all reports it as a 500 - which is how an unmatched URL ends up
 * looking like a server crash.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /** Everything the application throws on purpose already knows its status. */
    @ExceptionHandler(ApiException.class)
    public ResponseEntity<ErrorResponse> handleApiException(ApiException ex, HttpServletRequest request) {
        return build(ex.getStatus(), ex.getMessage(), request);
    }

    /**
     * Raised when a {@code @Valid @RequestBody} fails its Bean Validation
     * annotations. Every broken field is reported at once so the client does not
     * have to fix them one request at a time.
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException ex,
                                                          HttpServletRequest request) {
        List<ErrorResponse.FieldError> fieldErrors = ex.getBindingResult().getFieldErrors().stream()
                .map(error -> new ErrorResponse.FieldError(error.getField(), error.getDefaultMessage()))
                .toList();

        return ResponseEntity.badRequest().body(new ErrorResponse(
                Instant.now(),
                HttpStatus.BAD_REQUEST.value(),
                HttpStatus.BAD_REQUEST.getReasonPhrase(),
                "Request validation failed",
                request.getRequestURI(),
                fieldErrors));
    }

    /** No controller method matches the URL. A 404, not a 500. */
    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ErrorResponse> handleNoResource(NoResourceFoundException ex,
                                                          HttpServletRequest request) {
        return build(HttpStatus.NOT_FOUND, "No endpoint %s %s".formatted(
                request.getMethod(), request.getRequestURI()), request);
    }

    /** Right path, wrong verb - POST to something that only accepts GET. */
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ErrorResponse> handleMethodNotSupported(HttpRequestMethodNotSupportedException ex,
                                                                  HttpServletRequest request) {
        return build(HttpStatus.METHOD_NOT_ALLOWED, ex.getMessage(), request);
    }

    /**
     * The body is not readable at all - malformed JSON, or a value that cannot be
     * coerced into the target type. The raw message names Java classes and parser
     * internals, so it is not passed through to the caller.
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleUnreadableBody(HttpMessageNotReadableException ex,
                                                              HttpServletRequest request) {
        log.debug("Unreadable request body on {} {}", request.getMethod(), request.getRequestURI(), ex);

        // A value that cannot be converted - most often a typo in an enum - arrives here
        // rather than as a validation failure, because binding fails before Bean
        // Validation ever runs. Reporting it as "not valid JSON" would be wrong and
        // unhelpful: the JSON parsed fine, one field just did not fit its type.
        if (ex.getCause() instanceof InvalidFormatException cause) {
            String field = fieldPath(cause);
            String detail = cause.getTargetType() != null && cause.getTargetType().isEnum()
                    ? "must be one of " + Arrays.toString(cause.getTargetType().getEnumConstants())
                    : "must be a valid " + simpleTypeName(cause);

            return ResponseEntity.badRequest().body(new ErrorResponse(
                    Instant.now(),
                    HttpStatus.BAD_REQUEST.value(),
                    HttpStatus.BAD_REQUEST.getReasonPhrase(),
                    "Request validation failed",
                    request.getRequestURI(),
                    List.of(new ErrorResponse.FieldError(field,
                            "'%s' is not accepted here; %s".formatted(cause.getValue(), detail)))));
        }
        return build(HttpStatus.BAD_REQUEST, "Request body is missing or not valid JSON", request);
    }

    /** Rebuilds the dotted path Jackson walked, so the client is told which field failed. */
    private String fieldPath(InvalidFormatException cause) {
        StringBuilder path = new StringBuilder();
        cause.getPath().forEach(reference -> {
            if (reference.getPropertyName() != null) {
                if (!path.isEmpty()) {
                    path.append('.');
                }
                path.append(reference.getPropertyName());
            } else if (reference.getIndex() >= 0) {
                path.append('[').append(reference.getIndex()).append(']');
            }
        });
        return path.isEmpty() ? "body" : path.toString();
    }

    private String simpleTypeName(InvalidFormatException cause) {
        return cause.getTargetType() == null ? "value" : cause.getTargetType().getSimpleName();
    }

    /** A path variable or query parameter is the wrong type - /groups/abc where a number belongs. */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorResponse> handleTypeMismatch(MethodArgumentTypeMismatchException ex,
                                                            HttpServletRequest request) {
        return build(HttpStatus.BAD_REQUEST,
                "Parameter '%s' has an invalid value: %s".formatted(ex.getName(), ex.getValue()), request);
    }

    /**
     * A database constraint rejected the write - a duplicate email, or the same user
     * added to a group twice. Those constraints are the real guard, because two
     * concurrent requests can both pass an application-level check, so this handler
     * is what turns the race into a clean 409 rather than a 500.
     */
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ErrorResponse> handleDataIntegrity(DataIntegrityViolationException ex,
                                                             HttpServletRequest request) {
        log.warn("Constraint violation on {} {}", request.getMethod(), request.getRequestURI(), ex);
        return build(HttpStatus.CONFLICT, "That change conflicts with existing data", request);
    }

    /**
     * Raised by {@code @PreAuthorize} when a rule rejects the caller.
     *
     * <p>This handler is easy to miss and its absence is not obvious. Spring Security has
     * its own {@link com.satyam.splitly.auth.RestAccessDeniedHandler}, but that only sees
     * exceptions that reach the filter chain. Method security throws from an AOP proxy
     * around the controller, which is inside the dispatcher servlet - so the exception
     * unwinds to this advice instead, and without an entry here the catch-all below turns
     * every {@code @PreAuthorize} denial into a 500. Both handlers are needed: this one for
     * method security, that one for the URL rules.
     */
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleAccessDenied(AccessDeniedException ex,
                                                            HttpServletRequest request) {
        return build(HttpStatus.FORBIDDEN, "You are not allowed to do that", request);
    }

    /** The same problem for authentication failures surfacing inside the dispatcher. */
    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ErrorResponse> handleAuthentication(AuthenticationException ex,
                                                              HttpServletRequest request) {
        return build(HttpStatus.UNAUTHORIZED, "Authentication required", request);
    }

    /**
     * The catch-all. It logs the real stack trace and returns a generic message,
     * because exception text can leak table names, SQL, and file paths to callers.
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleUnexpected(Exception ex, HttpServletRequest request) {
        log.error("Unhandled exception on {} {}", request.getMethod(), request.getRequestURI(), ex);
        return build(HttpStatus.INTERNAL_SERVER_ERROR, "Something went wrong. Please try again.", request);
    }

    private ResponseEntity<ErrorResponse> build(HttpStatus status, String message, HttpServletRequest request) {
        return ResponseEntity.status(status).body(ErrorResponse.of(
                status.value(), status.getReasonPhrase(), message, request.getRequestURI()));
    }
}
