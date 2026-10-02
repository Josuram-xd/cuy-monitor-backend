package com.cuymonitor.backend.adapter.out.persistence.mapper;

import com.cuymonitor.backend.adapter.out.persistence.entity.OtpChallengeJpaEntity;
import com.cuymonitor.backend.domain.model.auth.OtpChallenge;
import org.springframework.stereotype.Component;

@Component
public class OtpChallengePersistenceMapper {

    public OtpChallengeJpaEntity toEntity(OtpChallenge challenge) {
        return new OtpChallengeJpaEntity(challenge.getId(), challenge.getUserId(), challenge.getCodeHash(),
                challenge.getCreatedAt(), challenge.getExpiresAt(), challenge.getMaxAttempts(),
                challenge.getAttempts(), challenge.getUsedAt().orElse(null), challenge.getRevokedAt().orElse(null));
    }

    public OtpChallenge toDomain(OtpChallengeJpaEntity entity) {
        return OtpChallenge.restore(entity.getId(), entity.getUserId(), entity.getCodeHash(), entity.getCreatedAt(),
                entity.getExpiresAt(), entity.getMaxAttempts(), entity.getAttempts(), entity.getUsedAt(),
                entity.getRevokedAt());
    }
}
