package com.cuymonitor.backend.adapter.in.web.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
        @NotBlank @Size(max = 50) String username,
        @NotBlank @Size(max = 150) String fullName,
        @NotBlank @Email @Size(max = 254) String email,
        // the real rules are in PasswordPolicy; the cap only stops a huge body from reaching it
        @NotBlank @Size(max = 200) String password) {

    @Override
    public String toString() {
        return "RegisterRequest[username=" + username + ", email=" + email + ", password=***]";
    }
}
