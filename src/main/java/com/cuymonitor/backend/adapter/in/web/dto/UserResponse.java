package com.cuymonitor.backend.adapter.in.web.dto;

import com.cuymonitor.backend.domain.model.user.User;
import com.cuymonitor.backend.domain.model.user.UserStatus;

import java.time.Instant;
import java.util.UUID;

public record UserResponse(UUID id, String username, String fullName, String email, UserStatus status,
                           Instant createdAt, Instant updatedAt) {

    public static UserResponse from(User user) {
        return new UserResponse(user.getId(), user.getUsername(), user.getFullName(), user.getEmail(),
                user.getStatus(), user.getCreatedAt(), user.getUpdatedAt());
    }
}
