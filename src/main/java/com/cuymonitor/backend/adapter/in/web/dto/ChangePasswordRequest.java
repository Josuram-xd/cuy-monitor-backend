package com.cuymonitor.backend.adapter.in.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

// currentPassword may be missing only for an account that has no password yet (made with Google).
// The real rules for newPassword are in PasswordPolicy; the caps only stop a huge body from reaching it.
public record ChangePasswordRequest(
        @Size(max = 200) String currentPassword,
        @NotBlank @Size(max = 200) String newPassword) {

    @Override
    public String toString() {
        return "ChangePasswordRequest[currentPassword=***, newPassword=***]";
    }
}
