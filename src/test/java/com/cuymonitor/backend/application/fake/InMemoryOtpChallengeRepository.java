package com.cuymonitor.backend.application.fake;

import com.cuymonitor.backend.domain.model.auth.OtpChallenge;
import com.cuymonitor.backend.domain.port.out.OtpChallengeRepository;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public class InMemoryOtpChallengeRepository implements OtpChallengeRepository {

    private final Map<UUID, OtpChallenge> challenges = new HashMap<>();

    @Override
    public OtpChallenge save(OtpChallenge otp) {
        challenges.put(otp.getId(), copy(otp));
        return copy(otp);
    }

    @Override
    public Optional<OtpChallenge> findById(UUID id) {
        return Optional.ofNullable(challenges.get(id)).map(InMemoryOtpChallengeRepository::copy);
    }

    @Override
    public List<OtpChallenge> findPendingByUserId(UUID userId) {
        return challenges.values().stream()
                .filter(c -> c.getUserId().equals(userId))
                .filter(c -> c.getUsedAt().isEmpty() && c.getRevokedAt().isEmpty())
                .map(InMemoryOtpChallengeRepository::copy)
                .toList();
    }

    private static OtpChallenge copy(OtpChallenge c) {
        return OtpChallenge.restore(c.getId(), c.getUserId(), c.getCodeHash(), c.getCreatedAt(), c.getExpiresAt(),
                c.getMaxAttempts(), c.getAttempts(), c.getUsedAt().orElse(null), c.getRevokedAt().orElse(null));
    }
}
