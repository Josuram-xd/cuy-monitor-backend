package com.cuymonitor.backend.adapter.in.web.dto;

import com.cuymonitor.backend.domain.model.user.User;
import com.cuymonitor.backend.domain.model.user.UserRole;

// only what the dashboard shows: no id, email, status or timestamps
// hasPassword: false for an account made with Google, so the dashboard does not ask for a password it never had
// role: the session cookie is HttpOnly, so the page cannot read the token's claim. This is how the dashboard
// knows whether to draw the admin section. It decides what is shown, never what is allowed: the backend checks
// the role again, against the database, on every admin call.
public record UserResponse(String username, String fullName, boolean hasPassword, UserRole role) {

    public static UserResponse from(User user) {
        return new UserResponse(user.getUsername(), user.getFullName(), user.hasPassword(), user.getRole());
    }
}
