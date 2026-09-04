package com.satyam.splitly.common;

import jakarta.persistence.Column;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.MappedSuperclass;
import java.time.Instant;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

/**
 * Adds created/updated timestamps that Spring Data fills in automatically.
 *
 * <p>Requires {@code @EnableJpaAuditing} somewhere in the context - see
 * {@link com.satyam.splitly.common.config.JpaAuditingConfig}. Without it these
 * fields stay null and the not-null constraint in the database rejects the insert.
 *
 * <p>Only entities with a meaningful edit history extend this. Rows like
 * group_members carry their own domain timestamp (joined_at) instead.
 */
@MappedSuperclass
@EntityListeners(AuditingEntityListener.class)
public abstract class AuditableEntity extends BaseEntity {

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
