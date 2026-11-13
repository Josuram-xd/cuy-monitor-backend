package com.cuymonitor.backend.adapter.in.web.dto;

import com.cuymonitor.backend.domain.model.user.User;
import com.cuymonitor.backend.domain.model.user.UserRole;

// only what the dashboard shows: no id, email, status or timestamps
// hasPassword: false for an account made with Google, so the dashboard does not ask for a password it never had
// role: the session cookie is HttpOnly, so the page cannot read the token's claim. This is how the dashboard
// knows whether to draw the admin section. It decides what is shown, never what is allowed: the backend checks
// the role again, against the database, on every admin call.
// nickname: null when the breeder never picked one. displayName is the one the screens print, so no
// screen has to repeat the "nickname, or the full name" rule.
// hasPhoto: the picture itself is at GET /api/v1/account/photo, never inside this answer
public record UserResponse(String username, String fullName, String nickname, String displayName,
                           boolean hasPassword, boolean hasPhoto, UserRole role) {

    public static UserResponse from(User user) {
        return new UserResponse(user.getUsername(), user.getFullName(), user.getNickname(), user.displayName(),
                user.hasPassword(), user.hasPhoto(), user.getRole());
    }
}
