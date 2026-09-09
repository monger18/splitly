package com.satyam.splitly.expense.dto;

import com.satyam.splitly.expense.Expense;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * List shape. Carries no shares, so listing stays one query per page rather than one per
 * expense - see {@code ExpenseRepository.findPageByGroupId}.
 */
public record ExpenseSummaryResponse(
        Long id, String description, BigDecimal amount,
        Long paidByUserId, String paidByName, String splitType, LocalDate expenseDate) {

    public static ExpenseSummaryResponse from(Expense expense) {
        return new ExpenseSummaryResponse(
                expense.getId(),
                expense.getDescription(),
                expense.getAmount(),
                expense.getPaidBy().getId(),
                expense.getPaidBy().getName(),
                expense.getSplitType().name(),
                expense.getExpenseDate());
    }
}
