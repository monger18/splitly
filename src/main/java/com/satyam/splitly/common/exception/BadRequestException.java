package com.satyam.splitly.common.exception;

import org.springframework.http.HttpStatus;

/**
 * 400. The request is well-formed but breaks a business rule that annotations
 * cannot express - percentages that do not add up to 100, a payer who is not in
 * the group, and so on.
 */
public class BadRequestException extends ApiException {

    public BadRequestException(String message) {
        super(HttpStatus.BAD_REQUEST, message);
    }
}
