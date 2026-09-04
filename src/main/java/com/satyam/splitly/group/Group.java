package com.satyam.splitly.group;

import com.satyam.splitly.common.AuditableEntity;
import com.satyam.splitly.user.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

/**
 * A set of people who share expenses - a trip, a flat, a dinner.
 *
 * <p>Mapped to expense_groups because GROUPS is a Postgres keyword.
 *
 * <p>Deliberately has no {@code List<GroupMember> members} field. A bidirectional
 * collection is convenient right up to the point where loading one group drags in
 * every member row; membership is read through {@link GroupMemberRepository}
 * instead, so each query asks for exactly what it needs.
 */
@Entity
@Table(name = "expense_groups")
@Getter
@Setter
public class Group extends AuditableEntity {

    @Column(nullable = false, length = 100)
    private String name;

    /** ISO 4217 code. One currency per group - no conversion anywhere in the app. */
    @Column(nullable = false, length = 3)
    private String currency;

    /**
     * LAZY is set explicitly because {@code @ManyToOne} defaults to EAGER, which
     * quietly fires an extra select every time a group is loaded. Every
     * association in this project is lazy for that reason.
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "created_by", nullable = false)
    private User createdBy;

    protected Group() {
    }

    public Group(String name, String currency, User createdBy) {
        this.name = name;
        this.currency = currency;
        this.createdBy = createdBy;
    }
}
