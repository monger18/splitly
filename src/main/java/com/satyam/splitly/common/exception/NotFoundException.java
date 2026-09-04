package com.satyam.splitly.common.exception;

import org.springframework.http.HttpStatus;

/** 404. The row does not exist, or the caller may not know that it does. */
public class NotFoundException extends ApiException {

    public NotFoundException(String message) {
        super(HttpStatus.NOT_FOUND, message);
    }

    public static NotFoundException of(String entity, Object id) {
        return new NotFoundException("%s %s not found".formatted(entity, id));
    }
}
