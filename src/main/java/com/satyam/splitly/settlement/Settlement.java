package com.satyam.splitly.settlement;

import com.satyam.splitly.common.BaseEntity;
import com.satyam.splitly.group.Group;
import com.satyam.splitly.user.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import lombok.Getter;
import lombok.Setter;

/**
 * Money that actually changed hands: "Bob paid Alice 450".
 *
 * <p>Kept apart from {@link com.satyam.splitly.expense.Expense} because it is a
 * different event. An expense creates debt; a settlement discharges it. Modelling
 * a settlement as a negative expense would corrupt every "what did we spend on
 * this trip" report.
 */
@Entity
@Table(name = "settlements")
@Getter
@Setter
public class Settlement extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "group_id", nullable = false)
    private Group group;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "from_user", nullable = false)
    private User fromUser;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "to_user", nullable = false)
    private User toUser;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal amount;

    @Column(length = 255)
    private String note;

    @Column(name = "settled_at", nullable = false)
    private Instant settledAt;

    protected Settlement() {
    }

    public Settlement(Group group, User fromUser, User toUser, BigDecimal amount, String note) {
        this.group = group;
        this.fromUser = fromUser;
        this.toUser = toUser;
        this.amount = amount;
        this.note = note;
        this.settledAt = Instant.now();
    }
}
