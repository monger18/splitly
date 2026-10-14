package com.satyam.splitly.group;

import com.satyam.splitly.common.auth.CurrentUser;
import com.satyam.splitly.common.exception.ForbiddenException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Transactional(readOnly = true)
public class GroupAccess {

    private final GroupMemberRepository groupMemberRepository;

    public GroupAccess(GroupMemberRepository groupMemberRepository) {
        this.groupMemberRepository = groupMemberRepository;
    }


    public GroupMember requireMember(Long groupId, Long userId) {
        return groupMemberRepository.findByGroupIdAndUserId(groupId, userId)
                .orElseThrow(() -> new ForbiddenException("You are not a member of group " + groupId));
    }

    public void requireAdmin(Long groupId, Long userId) {
        GroupMember member = requireMember(groupId, userId);
        if (member.getRole() != GroupRole.ADMIN) {
            throw new ForbiddenException("Only a group admin can do that");
        }
    }


    public boolean isMember(Long groupId) {
        return CurrentUser.id()
                .map(userId -> groupMemberRepository.existsByGroupIdAndUserId(groupId, userId))
                .orElse(false);
    }

    public boolean isAdmin(Long groupId) {
        return CurrentUser.id()
                .flatMap(userId -> groupMemberRepository.findByGroupIdAndUserId(groupId, userId))
                .map(member -> member.getRole() == GroupRole.ADMIN)
                .orElse(false);
    }
}
