package com.satyam.splitly.expense;

import com.satyam.splitly.common.AuditableEntity;
import com.satyam.splitly.group.Group;
import com.satyam.splitly.user.User;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import lombok.Getter;
import lombok.Setter;

/**
 * One thing somebody paid for, and how it is divided.
 *
 * <p>Single payer by design: one {@code paidBy}, not a list. Multi-payer expenses
 * are a possible extension, not part of v1.
 *
 * <p>Money is {@link BigDecimal}, never double. 0.1 + 0.2 is not 0.3 in binary
 * floating point, and an expense splitter that loses paise is a broken expense
 * splitter. The column is numeric(19,2) so the database agrees.
 */
@Entity
@Table(name = "expenses")
@Getter
@Setter
public class Expense extends AuditableEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "group_id", nullable = false)
    private Group group;

    @Column(nullable = false, length = 255)
    private String description;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal amount;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "paid_by", nullable = false)
    private User paidBy;

    @Enumerated(EnumType.STRING)
    @Column(name = "split_type", nullable = false, length = 20)
    private SplitType splitType;

    @Column(name = "expense_date", nullable = false)
    private LocalDate expenseDate;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "created_by", nullable = false)
    private User createdBy;

    /** Non-null once deleted. Balance queries skip these rows; history keeps them. */
    @Column(name = "deleted_at")
    private Instant deletedAt;

    /**
     * The shares belong to the expense and have no life without it, so the expense
     * is the aggregate root: cascade ALL saves them with the parent, and
     * orphanRemoval deletes a share the moment it leaves this list. That is what
     * lets {@code updateExpense} simply clear and refill the collection.
     */
    @OneToMany(mappedBy = "expense", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ExpenseShare> shares = new ArrayList<>();

    protected Expense() {
    }

    public Expense(Group group, String description, BigDecimal amount, User paidBy,
                   SplitType splitType, LocalDate expenseDate, User createdBy) {
        this.group = group;
        this.description = description;
        this.amount = amount;
        this.paidBy = paidBy;
        this.splitType = splitType;
        this.expenseDate = expenseDate;
        this.createdBy = createdBy;
    }

    public List<ExpenseShare> getShares() {
        return Collections.unmodifiableList(shares);
    }

    /**
     * Replaces the whole set of shares, keeping both sides of the association in
     * step. Callers must never mutate the list returned by {@link #getShares()};
     * with orphanRemoval on, a stale collection means deleted rows.
     */
    public void replaceShares(List<ExpenseShare> newShares) {
        this.shares.clear();
        newShares.forEach(share -> {
            share.setExpense(this);
            this.shares.add(share);
        });
    }

    public boolean isDeleted() {
        return deletedAt != null;
    }
}
