package com.satyam.splitly.expense;

import com.satyam.splitly.balance.GroupAmount;
import com.satyam.splitly.balance.UserAmount;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface ExpenseShareRepository extends JpaRepository<ExpenseShare, Long> {

    /**
     * Total owed by each member of a group.
     *
     * <p>One grouped query, not one per member. The alternative - loading every expense
     * and summing in Java - moves the whole table across the wire to compute a handful of
     * numbers the database can produce directly.
     *
     * <p>Soft-deleted expenses are excluded, which is what keeps a deleted expense from
     * still counting against someone's balance.
     */
    @Query("""
            select new com.satyam.splitly.balance.UserAmount(s.user.id, sum(s.shareAmount))
            from ExpenseShare s
            where s.expense.group.id = :groupId and s.expense.deletedAt is null
            group by s.user.id
            """)
    List<UserAmount> sumOwedByUser(Long groupId);

    /** The same total, but for one user across every group they belong to. */
    @Query("""
            select new com.satyam.splitly.balance.GroupAmount(s.expense.group.id, sum(s.shareAmount))
            from ExpenseShare s
            where s.user.id = :userId and s.expense.deletedAt is null
            group by s.expense.group.id
            """)
    List<GroupAmount> sumOwedByGroupForUser(Long userId);
}
