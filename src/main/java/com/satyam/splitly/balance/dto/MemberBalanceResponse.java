package com.satyam.splitly.balance.dto;

import java.math.BigDecimal;

/**
 * One member's net position in a group.
 *
 * @param net positive means the group owes them, negative means they owe the group.
 *            Across a whole group these always sum to exactly zero.
 */
public record MemberBalanceResponse(Long userId, String name, String email, BigDecimal net) {
}
