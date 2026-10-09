package com.cuymonitor.backend.adapter.out.persistence;

import com.cuymonitor.backend.adapter.out.persistence.mapper.OtpChallengePersistenceMapper;
import com.cuymonitor.backend.adapter.out.persistence.repository.OtpChallengeJpaRepository;
import com.cuymonitor.backend.domain.model.auth.OtpChallenge;
import com.cuymonitor.backend.domain.port.out.OtpChallengeRepository;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
public class OtpChallengePersistenceAdapter implements OtpChallengeRepository {

    private final OtpChallengeJpaRepository otpChallengeJpaRepository;
    private final OtpChallengePersistenceMapper mapper;

    public OtpChallengePersistenceAdapter(OtpChallengeJpaRepository otpChallengeJpaRepository,
                                          OtpChallengePersistenceMapper mapper) {
        this.otpChallengeJpaRepository = otpChallengeJpaRepository;
        this.mapper = mapper;
    }

    @Override
    public OtpChallenge save(OtpChallenge otp) {
        return mapper.toDomain(otpChallengeJpaRepository.save(mapper.toEntity(otp)));
    }

    @Override
    public Optional<OtpChallenge> findById(UUID id) {
        return otpChallengeJpaRepository.findById(id).map(mapper::toDomain);
    }

    @Override
    public List<OtpChallenge> findPendingByUserId(UUID userId) {
        return otpChallengeJpaRepository.findByUserIdAndUsedAtIsNullAndRevokedAtIsNull(userId).stream()
                .map(mapper::toDomain)
                .toList();
    }

    @Override
    public long countIssuedSince(UUID userId, Instant since) {
        return otpChallengeJpaRepository.countByUserIdAndCreatedAtGreaterThanEqual(userId, since);
    }
}
