package com.cuymonitor.backend.adapter.in.web.dto;

import jakarta.validation.constraints.NotBlank;

// currentPassword may be missing only for an account that has no password yet (made with Google)
public record ChangePasswordRequest(String currentPassword, @NotBlank String newPassword) {

    @Override
    public String toString() {
        return "ChangePasswordRequest[currentPassword=***, newPassword=***]";
    }
}
