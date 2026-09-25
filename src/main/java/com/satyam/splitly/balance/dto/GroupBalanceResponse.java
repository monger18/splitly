package com.satyam.splitly.balance.dto;

import java.math.BigDecimal;

/** The caller's net position in one group, for the across-all-groups view. */
public record GroupBalanceResponse(Long groupId, String groupName, String currency, BigDecimal net) {
}
