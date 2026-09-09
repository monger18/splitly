package com.satyam.splitly.expense.dto;

import com.satyam.splitly.expense.Expense;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/** Detail shape, including every participant's share. */
public record ExpenseResponse(
        Long id, Long groupId, String description, BigDecimal amount,
        Long paidByUserId, String paidByName, String splitType, LocalDate expenseDate,
        List<ExpenseShareResponse> shares) {

    public static ExpenseResponse from(Expense expense) {
        return new ExpenseResponse(
                expense.getId(),
                expense.getGroup().getId(),
                expense.getDescription(),
                expense.getAmount(),
                expense.getPaidBy().getId(),
                expense.getPaidBy().getName(),
                expense.getSplitType().name(),
                expense.getExpenseDate(),
                expense.getShares().stream().map(ExpenseShareResponse::from).toList());
    }
}
