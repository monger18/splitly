package com.satyam.splitly.expense.dto;

import com.satyam.splitly.expense.ExpenseShare;
import java.math.BigDecimal;

public record ExpenseShareResponse(Long userId, String name, BigDecimal amount) {

    public static ExpenseShareResponse from(ExpenseShare share) {
        return new ExpenseShareResponse(
                share.getUser().getId(), share.getUser().getName(), share.getShareAmount());
    }
}
