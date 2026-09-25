package com.satyam.splitly.balance.dto;

import java.math.BigDecimal;

/** A suggested payment: who should pay whom, and how much, to settle the group. */
public record TransferResponse(Long fromUserId, String fromName, Long toUserId, String toName, BigDecimal amount) {
}
