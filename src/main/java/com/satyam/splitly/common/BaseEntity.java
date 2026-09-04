package com.satyam.splitly.common;

import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;

/**
 * Shared identity for every entity.
 *
 * <p>The equals/hashCode pair here is deliberate and worth understanding. A JPA
 * entity changes identity over its lifetime: before {@code persist} its id is
 * null, afterwards it is set. If equals used the business fields, an entity
 * placed in a HashSet before saving would become unfindable after saving,
 * because its hash would change. So:
 *
 * <ul>
 *   <li>equals compares ids, and a null id is never equal to anything;</li>
 *   <li>hashCode is constant per class, so it survives the id being assigned.</li>
 * </ul>
 *
 * <p>This is also why we never put Lombok's {@code @Data} or {@code @EqualsAndHashCode}
 * on an entity: they would generate field-based versions and, worse, touch lazy
 * associations and trigger extra queries.
 */
@MappedSuperclass
public abstract class BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    public Long getId() {
        return id;
    }

    protected void setId(Long id) {
        this.id = id;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof BaseEntity that)) {
            return false;
        }
        // Different entity types are never equal, even with the same id.
        if (!getClass().equals(that.getClass())) {
            return false;
        }
        return id != null && id.equals(that.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
