package com.satyam.splitly.user;

import com.satyam.splitly.common.AuditableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

/**
 * A person who can belong to groups and take part in expenses.
 *
 * <p>{@code passwordHash} is nullable for now - registration with a real
 * password arrives with Spring Security in phase 2. It is never exposed through
 * a DTO.
 */
@Entity
@Table(name = "users")
@Getter
@Setter
public class User extends AuditableEntity {

    @Column(nullable = false, length = 100)
    private String name;

    @Column(nullable = false, length = 255)
    private String email;

    @Column(name = "password_hash", length = 255)
    private String passwordHash;

    /** JPA requires a no-arg constructor. Kept protected so callers use the real one. */
    protected User() {
    }

    public User(String name, String email) {
        this.name = name;
        this.email = email;
    }
}
