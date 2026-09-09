package com.satyam.splitly.common;

import java.util.List;
import java.util.function.Function;
import org.springframework.data.domain.Page;

/**
 * A stable JSON shape for a page of results.
 *
 * <p>Spring's own {@code Page} is not returned directly: its serialised form is an
 * internal structure that has changed between Spring Data versions, so exposing it makes
 * the API contract depend on a library upgrade. Newer versions warn about exactly this.
 */
public record PagedResponse<T>(List<T> content, int page, int size, long totalElements, int totalPages, boolean last) {

    public static <E, T> PagedResponse<T> of(Page<E> page, Function<E, T> mapper) {
        return new PagedResponse<>(
                page.getContent().stream().map(mapper).toList(),
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages(),
                page.isLast());
    }
}
