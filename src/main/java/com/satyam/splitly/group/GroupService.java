package com.satyam.splitly.group;

import com.satyam.splitly.common.exception.BadRequestException;
import com.satyam.splitly.common.exception.ConflictException;
import com.satyam.splitly.common.exception.NotFoundException;
import com.satyam.splitly.group.dto.CreateGroupRequest;
import com.satyam.splitly.user.User;
import com.satyam.splitly.user.UserService;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class GroupService {

    private final GroupRepository groupRepository;
    private final GroupMemberRepository groupMemberRepository;
    private final GroupAccess groupAccess;
    private final UserService userService;

    public GroupService(GroupRepository groupRepository,
                        GroupMemberRepository groupMemberRepository,
                        GroupAccess groupAccess,
                        UserService userService) {
        this.groupRepository = groupRepository;
        this.groupMemberRepository = groupMemberRepository;
        this.groupAccess = groupAccess;
        this.userService = userService;
    }

    /**
     * Creates the group and makes the creator its first member, as an admin.
     *
     * <p>Both writes happen in one transaction. If the membership insert fails the group
     * insert rolls back with it, because a group nobody belongs to is unreachable - no
     * caller would ever pass {@link GroupAccess#requireMember}, including its creator.
     */
    @Transactional
    public Group create(Long creatorId, CreateGroupRequest request) {
        User creator = userService.getById(creatorId);

        Group group = groupRepository.save(
                new Group(request.name().trim(), request.currencyOrDefault(), creator));

        groupMemberRepository.save(new GroupMember(group, creator, GroupRole.ADMIN));
        return group;
    }

    public List<GroupMember> listMemberships(Long userId) {
        return groupMemberRepository.findByUserIdWithGroup(userId);
    }

    public long countMembers(Long groupId) {
        return groupMemberRepository.countByGroupId(groupId);
    }

    public Group getForMember(Long groupId, Long callerId) {
        groupAccess.requireMember(groupId, callerId);
        return groupRepository.findById(groupId)
                .orElseThrow(() -> NotFoundException.of("Group", groupId));
    }

    public List<GroupMember> listMembers(Long groupId, Long callerId) {
        groupAccess.requireMember(groupId, callerId);
        return groupMemberRepository.findByGroupIdWithUser(groupId);
    }

    /** Only an admin may bring someone in. */
    @Transactional
    public GroupMember addMember(Long groupId, Long callerId, Long newMemberId) {
        groupAccess.requireAdmin(groupId, callerId);

        Group group = groupRepository.findById(groupId)
                .orElseThrow(() -> NotFoundException.of("Group", groupId));
        User user = userService.getById(newMemberId);

        // The unique index on (group_id, user_id) is the real guard against a race here;
        // this check exists so the ordinary case returns a readable 409.
        if (groupMemberRepository.existsByGroupIdAndUserId(groupId, newMemberId)) {
            throw new ConflictException("User " + newMemberId + " is already in this group");
        }
        return groupMemberRepository.save(new GroupMember(group, user, GroupRole.MEMBER));
    }

    /**
     * Removing a member. Anyone may leave; only an admin may remove somebody else.
     *
     * <p>The last admin cannot leave, because the group would then have no one able to
     * manage membership.
     *
     * <p>Not yet enforced: a member with an outstanding balance should not be removable,
     * since their share of past expenses would be orphaned. That check needs the balance
     * calculation and is added in phase 3 - it is checkpoint 3.7 on the roadmap, not an
     * oversight.
     */
    @Transactional
    public void removeMember(Long groupId, Long callerId, Long targetUserId) {
        boolean leavingSelf = callerId.equals(targetUserId);
        if (leavingSelf) {
            groupAccess.requireMember(groupId, callerId);
        } else {
            groupAccess.requireAdmin(groupId, callerId);
        }

        GroupMember target = groupMemberRepository.findByGroupIdAndUserId(groupId, targetUserId)
                .orElseThrow(() -> new NotFoundException(
                        "User " + targetUserId + " is not a member of group " + groupId));

        if (target.getRole() == GroupRole.ADMIN && countAdmins(groupId) == 1) {
            throw new BadRequestException("A group must keep at least one admin");
        }
        groupMemberRepository.delete(target);
    }

    private long countAdmins(Long groupId) {
        return groupMemberRepository.findByGroupIdWithUser(groupId).stream()
                .filter(member -> member.getRole() == GroupRole.ADMIN)
                .count();
    }
}
