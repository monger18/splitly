package com.satyam.splitly.group;

import com.satyam.splitly.common.auth.CurrentUser;
import com.satyam.splitly.common.exception.ForbiddenException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * The single place that answers "is this caller allowed into this group?".
 *
 * <p>Every group, expense, balance and settlement operation funnels through here. Kept
 * as one component rather than an {@code if} at the top of each service method, because
 * a rule that is written once cannot be forgotten in the one method that matters.
 *
 * <p>Two shapes of the same rule. The {@code require*} methods throw and hand back the
 * membership, for services that also need the member's role. The {@code is*} methods
 * return a boolean and read the caller from the security context, so they can be named
 * from {@code @PreAuthorize} - which rejects a request before the method body runs.
 *
 * <p>Both are kept deliberately. The annotation guards the HTTP entry point; the throwing
 * form still guards the service, which is also reachable from a scheduled job or another
 * service where there is no HTTP request at all. Defence in depth, with one implementation
 * of the rule behind both.
 */
@Component
@Transactional(readOnly = true)
public class GroupAccess {

    private final GroupMemberRepository groupMemberRepository;

    public GroupAccess(GroupMemberRepository groupMemberRepository) {
        this.groupMemberRepository = groupMemberRepository;
    }

    /**
     * 403 rather than 404 when the caller is not a member.
     *
     * <p>A stricter API would answer 404 to hide whether the group exists at all. 403 is
     * used here because group ids are sequential and not secret, and the clearer error
     * is worth more than the small amount of concealment.
     */
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

    /** For {@code @PreAuthorize("@groupAccess.isMember(#groupId)")}. */
    public boolean isMember(Long groupId) {
        return CurrentUser.id()
                .map(userId -> groupMemberRepository.existsByGroupIdAndUserId(groupId, userId))
                .orElse(false);
    }

    /** For {@code @PreAuthorize("@groupAccess.isAdmin(#groupId)")}. */
    public boolean isAdmin(Long groupId) {
        return CurrentUser.id()
                .flatMap(userId -> groupMemberRepository.findByGroupIdAndUserId(groupId, userId))
                .map(member -> member.getRole() == GroupRole.ADMIN)
                .orElse(false);
    }
}
