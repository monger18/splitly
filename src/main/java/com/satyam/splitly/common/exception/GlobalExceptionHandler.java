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


@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /** Everything the application throws on purpose already knows its status. */
    @ExceptionHandler(ApiException.class)
    public ResponseEntity<ErrorResponse> handleApiException(ApiException ex, HttpServletRequest request) {
        return build(ex.getStatus(), ex.getMessage(), request);
    }


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


    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleUnreadableBody(HttpMessageNotReadableException ex,
                                                              HttpServletRequest request) {
        log.debug("Unreadable request body on {} {}", request.getMethod(), request.getRequestURI(), ex);

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


    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ErrorResponse> handleDataIntegrity(DataIntegrityViolationException ex,
                                                             HttpServletRequest request) {
        log.warn("Constraint violation on {} {}", request.getMethod(), request.getRequestURI(), ex);
        return build(HttpStatus.CONFLICT, "That change conflicts with existing data", request);
    }


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
