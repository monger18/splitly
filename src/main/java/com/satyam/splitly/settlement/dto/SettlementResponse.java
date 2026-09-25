package com.satyam.splitly.settlement.dto;

import com.satyam.splitly.settlement.Settlement;
import java.math.BigDecimal;
import java.time.Instant;

public record SettlementResponse(Long id, Long fromUserId, String fromName,
                                 Long toUserId, String toName,
                                 BigDecimal amount, String note, Instant settledAt) {

    public static SettlementResponse from(Settlement settlement) {
        return new SettlementResponse(
                settlement.getId(),
                settlement.getFromUser().getId(), settlement.getFromUser().getName(),
                settlement.getToUser().getId(), settlement.getToUser().getName(),
                settlement.getAmount(), settlement.getNote(), settlement.getSettledAt());
    }
}
