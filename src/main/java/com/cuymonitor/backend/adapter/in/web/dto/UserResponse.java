package com.cuymonitor.backend.adapter.in.web.dto;

import com.cuymonitor.backend.domain.model.user.User;

// only what the dashboard shows: no id, email, status or timestamps
// hasPassword: false for an account made with Google, so the dashboard does not ask for a password it never had
public record UserResponse(String username, String fullName, boolean hasPassword) {

    public static UserResponse from(User user) {
        return new UserResponse(user.getUsername(), user.getFullName(), user.hasPassword());
    }
}
