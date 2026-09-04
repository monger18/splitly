package com.satyam.splitly.common.exception;

import org.springframework.http.HttpStatus;

/** 403. The caller is known but is not a member of the group they are reaching into. */
public class ForbiddenException extends ApiException {

    public ForbiddenException(String message) {
        super(HttpStatus.FORBIDDEN, message);
    }
}
