package com.satyam.splitly.expense;

import com.satyam.splitly.balance.GroupAmount;
import com.satyam.splitly.balance.UserAmount;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface ExpenseRepository extends JpaRepository<Expense, Long> {

    /**
     * A page of expenses with the payer already loaded.
     *
     * <p>Only the to-one association is fetched. Join-fetching the shares collection here
     * would force Hibernate to read every matching row and paginate in memory, because a
     * collection join multiplies rows and SQL's LIMIT would then cut expenses in half.
     * The shares are served by {@link #findDetailById} instead, which is why the list and
     * detail endpoints return different shapes.
     *
     * <p>A separate countQuery is required: the fetch join is invalid in a count.
     */
    @Query(value = """
            select e from Expense e
              join fetch e.paidBy
            where e.group.id = :groupId and e.deletedAt is null
            """,
            countQuery = """
            select count(e) from Expense e
            where e.group.id = :groupId and e.deletedAt is null
            """)
    Page<Expense> findPageByGroupId(Long groupId, Pageable pageable);

    /** Every read filters out soft-deleted rows; only the audit trail sees them. */
    Optional<Expense> findByIdAndDeletedAtIsNull(Long id);

    /** Total actually paid out by each member of a group. */
    @Query("""
            select new com.satyam.splitly.balance.UserAmount(e.paidBy.id, sum(e.amount))
            from Expense e
            where e.group.id = :groupId and e.deletedAt is null
            group by e.paidBy.id
            """)
    List<UserAmount> sumPaidByUser(Long groupId);

    @Query("""
            select new com.satyam.splitly.balance.GroupAmount(e.group.id, sum(e.amount))
            from Expense e
            where e.paidBy.id = :userId and e.deletedAt is null
            group by e.group.id
            """)
    List<GroupAmount> sumPaidByGroupForUser(Long userId);

    @Query("""
            select e from Expense e
              join fetch e.group
              join fetch e.paidBy
              left join fetch e.shares s
              left join fetch s.user
            where e.id = :id and e.deletedAt is null
            """)
    Optional<Expense> findDetailById(Long id);
}
