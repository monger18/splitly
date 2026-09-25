package com.satyam.splitly.balance;

import java.math.BigDecimal;

/** One aggregate row: a group and a summed amount, for one user across all their groups. */
public record GroupAmount(Long groupId, BigDecimal amount) {
}
