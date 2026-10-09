package com.cuymonitor.backend.adapter.in.web.dto;

import com.cuymonitor.backend.domain.model.user.User;

// only what the dashboard shows: no id, email, status or timestamps
public record UserResponse(String username, String fullName) {

    public static UserResponse from(User user) {
        return new UserResponse(user.getUsername(), user.getFullName());
    }
}
