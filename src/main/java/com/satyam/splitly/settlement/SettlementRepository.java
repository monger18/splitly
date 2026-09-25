package com.satyam.splitly.settlement;

import com.satyam.splitly.balance.GroupAmount;
import com.satyam.splitly.balance.UserAmount;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface SettlementRepository extends JpaRepository<Settlement, Long> {

    @Query("""
            select s from Settlement s
              join fetch s.fromUser
              join fetch s.toUser
            where s.group.id = :groupId
            order by s.settledAt desc
            """)
    List<Settlement> findByGroupIdWithUsers(Long groupId);

    /** Total each member has handed over to settle up. */
    @Query("""
            select new com.satyam.splitly.balance.UserAmount(s.fromUser.id, sum(s.amount))
            from Settlement s where s.group.id = :groupId group by s.fromUser.id
            """)
    List<UserAmount> sumPaidOutByUser(Long groupId);

    /** Total each member has received. */
    @Query("""
            select new com.satyam.splitly.balance.UserAmount(s.toUser.id, sum(s.amount))
            from Settlement s where s.group.id = :groupId group by s.toUser.id
            """)
    List<UserAmount> sumReceivedByUser(Long groupId);

    @Query("""
            select new com.satyam.splitly.balance.GroupAmount(s.group.id, sum(s.amount))
            from Settlement s where s.fromUser.id = :userId group by s.group.id
            """)
    List<GroupAmount> sumPaidOutByGroupForUser(Long userId);

    @Query("""
            select new com.satyam.splitly.balance.GroupAmount(s.group.id, sum(s.amount))
            from Settlement s where s.toUser.id = :userId group by s.group.id
            """)
    List<GroupAmount> sumReceivedByGroupForUser(Long userId);
}
