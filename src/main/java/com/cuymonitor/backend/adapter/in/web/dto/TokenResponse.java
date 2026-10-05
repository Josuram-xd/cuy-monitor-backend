package com.cuymonitor.backend.adapter.in.web.dto;

import com.cuymonitor.backend.domain.model.auth.AuthToken;

import java.time.Instant;

public record TokenResponse(String accessToken, String tokenType, Instant expiresAt) {

    public static TokenResponse from(AuthToken token) {
        return new TokenResponse(token.accessToken(), "Bearer", token.expiresAt());
    }

    @Override
    public String toString() {
        return "TokenResponse[accessToken=***, tokenType=" + tokenType + ", expiresAt=" + expiresAt + "]";
    }
}
