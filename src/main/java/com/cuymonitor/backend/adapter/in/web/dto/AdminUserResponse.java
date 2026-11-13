package com.cuymonitor.backend.adapter.in.web.dto;

import com.cuymonitor.backend.domain.model.user.User;
import com.cuymonitor.backend.domain.model.user.UserRole;
import com.cuymonitor.backend.domain.model.user.UserStatus;

import java.time.Instant;
import java.util.UUID;

/** An account as the admin screen shows it. No password hash and no Google subject: the screen never needs them. */
public record AdminUserResponse(UUID id, String username, String fullName, String email, UserStatus status,
                                UserRole role, Instant createdAt) {

    public static AdminUserResponse from(User user) {
        return new AdminUserResponse(user.getId(), user.getUsername(), user.getFullName(), user.getEmail(),
                user.getStatus(), user.getRole(), user.getCreatedAt());
    }
}
