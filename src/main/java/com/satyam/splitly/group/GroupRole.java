package com.satyam.splitly.group;

/** What a member is allowed to do inside a group. */
public enum GroupRole {
    /** Created the group; can add and remove members. */
    ADMIN,
    /** Can add expenses and settle up. */
    MEMBER
}
