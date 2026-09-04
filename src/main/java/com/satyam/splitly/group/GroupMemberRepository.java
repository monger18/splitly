package com.satyam.splitly.group;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface GroupMemberRepository extends JpaRepository<GroupMember, Long> {

    boolean existsByGroupIdAndUserId(Long groupId, Long userId);

    Optional<GroupMember> findByGroupIdAndUserId(Long groupId, Long userId);

    /**
     * "join fetch" pulls the user rows in the same select as the memberships.
     * Without it, rendering a 10-member group runs 1 query for the memberships
     * plus 10 more for the lazy users - the N+1 problem, and the single most
     * common performance bug in JPA code.
     */
    @Query("select m from GroupMember m join fetch m.user where m.group.id = :groupId")
    List<GroupMember> findByGroupIdWithUser(Long groupId);

    @Query("select m from GroupMember m join fetch m.group where m.user.id = :userId")
    List<GroupMember> findByUserIdWithGroup(Long userId);

    long countByGroupId(Long groupId);
}
