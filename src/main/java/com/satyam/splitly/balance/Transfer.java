package com.satyam.splitly.balance;

import java.math.BigDecimal;

/** One payment that would settle part of a group: {@code fromUserId} pays {@code toUserId}. */
public record Transfer(Long fromUserId, Long toUserId, BigDecimal amount) {
}
