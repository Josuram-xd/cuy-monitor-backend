package com.cuymonitor.backend.domain.port.out;

import com.cuymonitor.backend.domain.model.auth.OtpChallenge;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface OtpChallengeRepository {
    OtpChallenge save(OtpChallenge otp);
    Optional<OtpChallenge> findById(UUID id);
    List<OtpChallenge> findPendingByUserId(UUID userId);
}
