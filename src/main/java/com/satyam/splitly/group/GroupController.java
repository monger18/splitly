package com.satyam.splitly.group;

import com.satyam.splitly.common.auth.CurrentUserId;
import com.satyam.splitly.group.dto.AddMemberRequest;
import com.satyam.splitly.group.dto.CreateGroupRequest;
import com.satyam.splitly.group.dto.GroupDetailResponse;
import com.satyam.splitly.group.dto.GroupMemberResponse;
import com.satyam.splitly.group.dto.GroupResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/groups")
@Tag(name = "Groups")
public class GroupController {

    private final GroupService groupService;

    public GroupController(GroupService groupService) {
        this.groupService = groupService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create a group", description = "The caller becomes its first member, as admin.")
    public GroupResponse create(@CurrentUserId Long userId, @Valid @RequestBody CreateGroupRequest request) {
        Group group = groupService.create(userId, request);
        return GroupResponse.from(group, 1);
    }

    @GetMapping
    @Operation(summary = "Groups the caller belongs to")
    public List<GroupResponse> myGroups(@CurrentUserId Long userId) {
        return groupService.listMemberships(userId).stream()
                .map(membership -> {
                    Group group = membership.getGroup();
                    return GroupResponse.from(group, groupService.countMembers(group.getId()));
                })
                .toList();
    }

    @GetMapping("/{groupId}")
    @Operation(summary = "One group, with its members")
    public GroupDetailResponse detail(@CurrentUserId Long userId, @PathVariable Long groupId) {
        return GroupDetailResponse.from(
                groupService.getForMember(groupId, userId),
                groupService.listMembers(groupId, userId));
    }

    /**
     * Evaluated before the method runs, so a non-admin never reaches the service.
     * {@code #groupId} refers to the parameter by name, which works because
     * spring-boot-starter-parent compiles with -parameters.
     */
    @PostMapping("/{groupId}/members")
    @PreAuthorize("@groupAccess.isAdmin(#groupId)")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Add a member", description = "Admins only.")
    public GroupMemberResponse addMember(@CurrentUserId Long userId,
                                         @PathVariable Long groupId,
                                         @Valid @RequestBody AddMemberRequest request) {
        return GroupMemberResponse.from(groupService.addMember(groupId, userId, request.userId()));
    }

    @DeleteMapping("/{groupId}/members/{targetUserId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Remove a member", description = "Anyone may leave; only an admin may remove someone else.")
    public void removeMember(@CurrentUserId Long userId,
                             @PathVariable Long groupId,
                             @PathVariable Long targetUserId) {
        groupService.removeMember(groupId, userId, targetUserId);
    }
}
