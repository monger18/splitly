package com.satyam.splitly.balance;

import com.satyam.splitly.balance.dto.GroupBalanceResponse;
import com.satyam.splitly.balance.dto.MemberBalanceResponse;
import com.satyam.splitly.balance.dto.TransferResponse;
import com.satyam.splitly.common.auth.CurrentUserId;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
@Tag(name = "Balances")
public class BalanceController {

    private final BalanceService balanceService;

    public BalanceController(BalanceService balanceService) {
        this.balanceService = balanceService;
    }

    @GetMapping("/groups/{groupId}/balances")
    @Operation(summary = "Net position of each member",
            description = "Positive means the group owes them. These always sum to zero.")
    public List<MemberBalanceResponse> groupBalances(@CurrentUserId Long userId, @PathVariable Long groupId) {
        return balanceService.groupBalances(groupId, userId);
    }

    @GetMapping("/groups/{groupId}/balances/simplified")
    @Operation(summary = "The shortest set of payments that settles the group",
            description = "Greedy minimum cash flow: at most one fewer payment than there are people with a balance.")
    public List<TransferResponse> simplified(@CurrentUserId Long userId, @PathVariable Long groupId) {
        return balanceService.simplified(groupId, userId);
    }

    @GetMapping("/users/me/balances")
    @Operation(summary = "The caller's net position in every group they belong to")
    public List<GroupBalanceResponse> myBalances(@CurrentUserId Long userId) {
        return balanceService.myBalances(userId);
    }
}
