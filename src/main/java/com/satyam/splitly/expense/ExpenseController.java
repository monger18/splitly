package com.satyam.splitly.expense;

import com.satyam.splitly.common.PagedResponse;
import com.satyam.splitly.common.auth.CurrentUserId;
import com.satyam.splitly.expense.dto.CreateExpenseRequest;
import com.satyam.splitly.expense.dto.ExpenseResponse;
import com.satyam.splitly.expense.dto.ExpenseSummaryResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
@Tag(name = "Expenses")
public class ExpenseController {

    private final ExpenseService expenseService;

    public ExpenseController(ExpenseService expenseService) {
        this.expenseService = expenseService;
    }

    @PostMapping("/groups/{groupId}/expenses")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Add an expense to a group")
    public ExpenseResponse create(@CurrentUserId Long userId,
                                  @PathVariable Long groupId,
                                  @Valid @RequestBody CreateExpenseRequest request) {
        return ExpenseResponse.from(expenseService.create(groupId, userId, request));
    }

    /**
     * {@code ?page=0&size=20&sort=amount,desc} works out of the box - Spring Data builds
     * the Pageable from the query string. The default here is newest first.
     */
    @GetMapping("/groups/{groupId}/expenses")
    @Operation(summary = "List a group's expenses, newest first")
    public PagedResponse<ExpenseSummaryResponse> list(
            @CurrentUserId Long userId,
            @PathVariable Long groupId,
            @PageableDefault(size = 20, sort = "expenseDate", direction = Sort.Direction.DESC) Pageable pageable) {

        return PagedResponse.of(expenseService.list(groupId, userId, pageable), ExpenseSummaryResponse::from);
    }

    @GetMapping("/expenses/{expenseId}")
    @Operation(summary = "One expense, with every participant's share")
    public ExpenseResponse detail(@CurrentUserId Long userId, @PathVariable Long expenseId) {
        return ExpenseResponse.from(expenseService.getDetail(expenseId, userId));
    }

    @PutMapping("/expenses/{expenseId}")
    @Operation(summary = "Replace an expense", description = "The person who added it, or a group admin.")
    public ExpenseResponse update(@CurrentUserId Long userId,
                                  @PathVariable Long expenseId,
                                  @Valid @RequestBody CreateExpenseRequest request) {
        expenseService.update(expenseId, userId, request);
        return ExpenseResponse.from(expenseService.getDetail(expenseId, userId));
    }

    @DeleteMapping("/expenses/{expenseId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Delete an expense", description = "Soft delete; the row is kept for history.")
    public void delete(@CurrentUserId Long userId, @PathVariable Long expenseId) {
        expenseService.delete(expenseId, userId);
    }
}
