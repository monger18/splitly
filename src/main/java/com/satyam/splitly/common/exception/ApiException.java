package com.satyam.splitly.common.exception;

import org.springframework.http.HttpStatus;

/**
 * Base for every error this application raises on purpose.
 *
 * <p>Carrying the HTTP status on the exception keeps the decision next to the
 * reason. The alternative - services returning result objects that each layer
 * inspects and rethrows - buries the happy path in error plumbing.
 */
public abstract class ApiException extends RuntimeException {

    private final HttpStatus status;

    protected ApiException(HttpStatus status, String message) {
        super(message);
        this.status = status;
    }

    public HttpStatus getStatus() {
        return status;
    }
}
