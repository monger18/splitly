package com.satyam.splitly.common.exception;

import org.springframework.http.HttpStatus;

// 409. The request clashes with current state - a duplicate email, a member who is already in the group.
public class ConflictException extends ApiException {

    public ConflictException(String message) {
        super(HttpStatus.CONFLICT, message);
    }
}
