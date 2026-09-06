package com.satyam.splitly.group.dto;

import com.satyam.splitly.group.GroupMember;
import java.time.Instant;

public record GroupMemberResponse(Long userId, String name, String email, String role, Instant joinedAt) {

    /**
     * Reads {@code member.getUser()}, so the caller must have loaded the membership with
     * the user already fetched - see
     * {@code GroupMemberRepository.findByGroupIdWithUser}. With
     * {@code open-in-view: false} a lazy user here would throw rather than quietly
     * firing one extra query per member.
     */
    public static GroupMemberResponse from(GroupMember member) {
        return new GroupMemberResponse(
                member.getUser().getId(),
                member.getUser().getName(),
                member.getUser().getEmail(),
                member.getRole().name(),
                member.getJoinedAt());
    }
}
