package com.satyam.splitly.group.dto;

import jakarta.validation.constraints.NotNull;

public record AddMemberRequest(

        @NotNull(message = "userId is required")
        Long userId) {
}
