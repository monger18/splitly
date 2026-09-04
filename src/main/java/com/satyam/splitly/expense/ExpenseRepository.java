package com.satyam.splitly.expense;

import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface ExpenseRepository extends JpaRepository<Expense, Long> {

    /** Every read filters out soft-deleted rows; only the audit trail sees them. */
    Page<Expense> findByGroupIdAndDeletedAtIsNull(Long groupId, Pageable pageable);

    Optional<Expense> findByIdAndDeletedAtIsNull(Long id);

    @Query("""
            select e from Expense e
              join fetch e.paidBy
              left join fetch e.shares s
              left join fetch s.user
            where e.id = :id and e.deletedAt is null
            """)
    Optional<Expense> findDetailById(Long id);
}
