package com.satyam.splitly.user.dto;

import com.satyam.splitly.user.User;

/**
 * What the API gives back for a user.
 *
 * <p>Entities never leave the service layer, and this type is why. Returning
 * {@link User} directly would serialise whatever fields it happens to have - today that
 * includes {@code passwordHash} - and would tie the JSON contract to the database
 * schema, so renaming a column would break every client. It would also try to serialise
 * lazy associations after the persistence context has closed.
 */
public record UserResponse(Long id, String name, String email) {

    public static UserResponse from(User user) {
        return new UserResponse(user.getId(), user.getName(), user.getEmail());
    }
}
