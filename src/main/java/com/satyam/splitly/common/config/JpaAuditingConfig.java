package com.satyam.splitly.common.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

/**
 * Turns on the {@code @CreatedDate} / {@code @LastModifiedDate} support used by
 * {@link com.satyam.splitly.common.AuditableEntity}.
 *
 * <p>Kept in its own class rather than on the main application class so that
 * slice tests can opt in or out of it.
 */
@Configuration
@EnableJpaAuditing
public class JpaAuditingConfig {
}
