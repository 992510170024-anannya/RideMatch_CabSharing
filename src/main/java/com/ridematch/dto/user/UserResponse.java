package com.ridematch.dto.user;

import com.ridematch.domain.entity.User;
import com.ridematch.domain.enums.UserRole;

/** Safe account representation; deliberately excludes password hash and credentials. */
public record UserResponse(Long id, String fullName, String email, UserRole role) {

    public static UserResponse from(User user) {
        return new UserResponse(user.getId(), user.getFullName(), user.getEmail(), user.getRole());
    }
}
