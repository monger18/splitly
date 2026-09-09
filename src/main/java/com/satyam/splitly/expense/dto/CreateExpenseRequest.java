package com.satyam.splitly.expense.dto;

import com.satyam.splitly.expense.SplitType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record CreateExpenseRequest(

        @NotBlank(message = "Description is required")
        @Size(max = 255, message = "Description must be at most 255 characters")
        String description,

        @NotNull(message = "Amount is required")
        @DecimalMin(value = "0.01", message = "Amount must be greater than zero")
        @Digits(integer = 17, fraction = 2, message = "Amount can have at most 2 decimal places")
        BigDecimal amount,

        @NotNull(message = "paidBy is required")
        Long paidBy,

        @NotNull(message = "splitType is required")
        SplitType splitType,

        @NotNull(message = "expenseDate is required")
        LocalDate expenseDate,

        /**
         * {@code @Valid} on the collection is what makes the annotations inside
         * ParticipantRequest run. Without it the inner records are bound but never
         * checked - a quiet and common mistake.
         */
        @NotEmpty(message = "At least one participant is required")
        @Valid
        List<ParticipantRequest> participants) {
}
