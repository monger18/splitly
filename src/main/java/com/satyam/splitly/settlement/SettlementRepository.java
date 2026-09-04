package com.satyam.splitly.settlement;

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
}
