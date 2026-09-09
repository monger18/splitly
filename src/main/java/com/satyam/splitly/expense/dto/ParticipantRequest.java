package com.satyam.splitly.expense.dto;

import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

/**
 * @param value meaning depends on the expense's split type: omitted for EQUAL, the exact
 *              amount for EXACT, the percentage for PERCENTAGE, the weight for SHARES.
 *              Not validated here because the requirement differs per type; the strategy
 *              enforces its own rule and reports a clear message.
 */
public record ParticipantRequest(

        @NotNull(message = "userId is required")
        Long userId,

        BigDecimal value) {
}
