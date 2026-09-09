package com.satyam.splitly.expense.split;

import java.math.BigDecimal;

/**
 * One participant, as supplied by the caller.
 *
 * @param userId who takes part
 * @param value  what it means depends on the split type: ignored for EQUAL, the exact
 *               amount for EXACT, the percentage for PERCENTAGE, the weight for SHARES.
 *               Each strategy validates its own requirement, so an EXACT split with a
 *               missing amount fails with a clear message rather than a
 *               NullPointerException.
 */
public record SplitInput(Long userId, BigDecimal value) {
}
