package com.satyam.splitly.balance;

import java.math.BigDecimal;

/** One aggregate row: a user and a summed amount. Populated by a JPQL constructor expression. */
public record UserAmount(Long userId, BigDecimal amount) {
}
