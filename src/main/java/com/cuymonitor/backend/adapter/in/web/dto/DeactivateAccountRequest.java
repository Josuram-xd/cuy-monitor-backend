package com.cuymonitor.backend.adapter.in.web.dto;

import jakarta.validation.constraints.NotBlank;

public record DeactivateAccountRequest(@NotBlank String currentPassword) {

    @Override
    public String toString() {
        return "DeactivateAccountRequest[currentPassword=***]";
    }
}
