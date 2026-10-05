package com.cuymonitor.backend.adapter.in.web.dto;

import com.cuymonitor.backend.domain.model.auth.LoginChallenge;

import java.time.Instant;
import java.util.UUID;

public record ChallengeResponse(UUID challengeId, Instant expiresAt) {

    public static ChallengeResponse from(LoginChallenge challenge) {
        return new ChallengeResponse(challenge.challengeId(), challenge.expiresAt());
    }
}
