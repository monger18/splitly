package com.satyam.splitly.settlement;

import com.satyam.splitly.common.auth.CurrentUserId;
import com.satyam.splitly.settlement.dto.CreateSettlementRequest;
import com.satyam.splitly.settlement.dto.SettlementResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/groups/{groupId}/settlements")
@Tag(name = "Settlements")
public class SettlementController {

    private final SettlementService settlementService;

    public SettlementController(SettlementService settlementService) {
        this.settlementService = settlementService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Record a payment between two members")
    public SettlementResponse record(@CurrentUserId Long userId,
                                     @PathVariable Long groupId,
                                     @Valid @RequestBody CreateSettlementRequest request) {
        return SettlementResponse.from(settlementService.record(groupId, userId, request));
    }

    @GetMapping
    @Operation(summary = "Every settlement in the group, most recent first")
    public List<SettlementResponse> list(@CurrentUserId Long userId, @PathVariable Long groupId) {
        return settlementService.list(groupId, userId).stream().map(SettlementResponse::from).toList();
    }
}
