package com.satyam.splitly.group;

import com.satyam.splitly.common.BaseEntity;
import com.satyam.splitly.user.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;
import lombok.Getter;
import lombok.Setter;

/**
 * Membership of a user in a group.
 *
 * <p>This is a real entity rather than a plain {@code @ManyToMany} because the
 * relationship carries data of its own - a role and a join date. A {@code @ManyToMany}
 * can only map a bare join table, so the moment the link needs a field you model
 * it explicitly. That is almost always, which is why {@code @ManyToMany} is rare
 * in production code.
 */
@Entity
@Table(name = "group_members")
@Getter
@Setter
public class GroupMember extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "group_id", nullable = false)
    private Group group;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    /**
     * STRING, never the ORDINAL default. Ordinal stores the enum's position, so
     * reordering or inserting a constant silently rewrites the meaning of every
     * existing row.
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private GroupRole role;

    @Column(name = "joined_at", nullable = false)
    private Instant joinedAt;

    protected GroupMember() {
    }

    public GroupMember(Group group, User user, GroupRole role) {
        this.group = group;
        this.user = user;
        this.role = role;
        this.joinedAt = Instant.now();
    }
}
