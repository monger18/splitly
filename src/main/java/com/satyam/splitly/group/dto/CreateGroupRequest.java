package com.satyam.splitly.group.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * @param currency optional ISO 4217 code; defaults to INR. One currency per group, fixed
 *                 at creation, because the application does no conversion anywhere.
 */
public record CreateGroupRequest(

        @NotBlank(message = "Name is required")
        @Size(max = 100, message = "Name must be at most 100 characters")
        String name,

        @Pattern(regexp = "^[A-Z]{3}$", message = "Currency must be a 3-letter ISO code, e.g. INR")
        String currency) {

    public String currencyOrDefault() {
        return currency == null || currency.isBlank() ? "INR" : currency;
    }
}
