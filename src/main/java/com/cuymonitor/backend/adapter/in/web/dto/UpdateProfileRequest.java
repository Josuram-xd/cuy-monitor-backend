package com.cuymonitor.backend.adapter.in.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import com.cuymonitor.backend.domain.model.user.User;

/** nickname is optional: sending null or blank clears it, and the screens fall back to the full name. */
public record UpdateProfileRequest(@NotBlank @Size(max = 150) String fullName,
                                   @Size(max = User.MAX_NICKNAME_LENGTH) String nickname) {
}
