package com.satyam.splitly.common.exception;

import org.springframework.http.HttpStatus;

/** 401. The caller did not identify themselves at all. */
public class UnauthorizedException extends ApiException {

    public UnauthorizedException(String message) {
        super(HttpStatus.UNAUTHORIZED, message);
    }
}
