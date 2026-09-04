package com.satyam.splitly.expense;

import com.satyam.splitly.common.BaseEntity;
import com.satyam.splitly.user.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import lombok.Getter;
import lombok.Setter;

/**
 * How much one participant owes for one expense.
 *
 * <p>The payer normally has a share too - if Alice pays 900 for three people, she
 * owes 300 of it herself and is out of pocket by 600. Balances fall out of that
 * arithmetic rather than being a special case.
 */
@Entity
@Table(name = "expense_shares")
@Getter
@Setter
public class ExpenseShare extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "expense_id", nullable = false)
    private Expense expense;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "share_amount", nullable = false, precision = 19, scale = 2)
    private BigDecimal shareAmount;

    protected ExpenseShare() {
    }

    public ExpenseShare(User user, BigDecimal shareAmount) {
        this.user = user;
        this.shareAmount = shareAmount;
    }
}
