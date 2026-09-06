package com.satyam.splitly.group.dto;

import com.satyam.splitly.group.Group;

/** Summary shape, used when listing the caller's groups. */
public record GroupResponse(Long id, String name, String currency, long memberCount) {

    public static GroupResponse from(Group group, long memberCount) {
        return new GroupResponse(group.getId(), group.getName(), group.getCurrency(), memberCount);
    }
}
