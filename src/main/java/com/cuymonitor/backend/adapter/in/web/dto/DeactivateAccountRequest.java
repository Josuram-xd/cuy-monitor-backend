package com.cuymonitor.backend.adapter.in.web.dto;

import jakarta.validation.constraints.Size;

// currentPassword may be missing only for an account that has no password (made with Google)
public record DeactivateAccountRequest(@Size(max = 200) String currentPassword) {

    @Override
    public String toString() {
        return "DeactivateAccountRequest[currentPassword=***]";
    }
}
