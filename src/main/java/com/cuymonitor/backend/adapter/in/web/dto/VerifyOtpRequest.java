package com.cuymonitor.backend.adapter.in.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

import java.util.UUID;

public record VerifyOtpRequest(@NotNull UUID challengeId, @NotBlank @Pattern(regexp = "\\d{6}") String code) {

    @Override
    public String toString() {
        return "VerifyOtpRequest[challengeId=" + challengeId + ", code=***]";
    }
}
