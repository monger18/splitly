package com.satyam.splitly.user.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Incoming body for creating a user.
 *
 * <p>A record, not a class: it is immutable data with no behaviour, so there is nothing
 * for Lombok to generate. Jackson binds JSON straight into the constructor.
 *
 * <p>The annotations are Bean Validation. They only run because the controller parameter
 * is marked {@code @Valid} - without that they are silently ignored, which is a common
 * and very quiet bug. Failures are collected by
 * {@link com.satyam.splitly.common.exception.GlobalExceptionHandler} and returned as one
 * 400 listing every bad field.
 *
 * <p>No password field yet; it arrives with Spring Security in phase 4.
 */
public record RegisterUserRequest(

        @NotBlank(message = "Name is required")
        @Size(max = 100, message = "Name must be at most 100 characters")
        String name,

        @NotBlank(message = "Email is required")
        @Email(message = "Email must be a valid address")
        @Size(max = 255, message = "Email must be at most 255 characters")
        String email) {
}
