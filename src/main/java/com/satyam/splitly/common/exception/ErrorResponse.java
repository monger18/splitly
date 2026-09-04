package com.satyam.splitly.common.exception;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.time.Instant;
import java.util.List;

/**
 * The single error shape every failing endpoint returns.
 *
 * <p>A record, because it is immutable data with no behaviour - no Lombok, no
 * boilerplate. One consistent envelope means a client can write one error handler
 * instead of guessing at each endpoint's format.
 *
 * @param fieldErrors populated only for validation failures; omitted otherwise
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ErrorResponse(
        Instant timestamp,
        int status,
        String error,
        String message,
        String path,
        List<FieldError> fieldErrors) {

    public record FieldError(String field, String message) {
    }

    public static ErrorResponse of(int status, String error, String message, String path) {
        return new ErrorResponse(Instant.now(), status, error, message, path, null);
    }
}
