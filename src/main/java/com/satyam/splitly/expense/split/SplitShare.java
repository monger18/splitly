package com.satyam.splitly.expense.split;

import java.math.BigDecimal;

/** What one participant owes for one expense, after the split has been computed. */
public record SplitShare(Long userId, BigDecimal amount) {
}
