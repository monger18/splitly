package com.satyam.splitly.group.dto;

import com.satyam.splitly.group.Group;
import com.satyam.splitly.group.GroupMember;
import java.util.List;

/** Full shape, used for a single group. Separate from the summary so listing stays cheap. */
public record GroupDetailResponse(Long id, String name, String currency, List<GroupMemberResponse> members) {

    public static GroupDetailResponse from(Group group, List<GroupMember> members) {
        return new GroupDetailResponse(
                group.getId(),
                group.getName(),
                group.getCurrency(),
                members.stream().map(GroupMemberResponse::from).toList());
    }
}
