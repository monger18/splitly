package com.satyam.splitly.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RegisterRequest(

        @NotBlank(message = "Name is required")
        @Size(max = 100, message = "Name must be at most 100 characters")
        String name,

        @NotBlank(message = "Email is required")
        @Email(message = "Email must be a valid address")
        @Size(max = 255, message = "Email must be at most 255 characters")
        String email,

        /**
         * A minimum length and nothing more. Composition rules - one capital, one digit,
         * one symbol - push people towards predictable substitutions and shorter
         * passwords, and current NIST guidance drops them in favour of length. The upper
         * bound exists because BCrypt silently ignores anything past 72 bytes, so a longer
         * password would give a false sense of strength.
         */
        @NotBlank(message = "Password is required")
        @Size(min = 8, max = 72, message = "Password must be between 8 and 72 characters")
        String password) {
}
