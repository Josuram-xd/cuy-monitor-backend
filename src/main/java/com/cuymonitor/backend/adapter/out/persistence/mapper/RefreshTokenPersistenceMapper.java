package com.cuymonitor.backend.adapter.out.persistence.mapper;

import com.cuymonitor.backend.adapter.out.persistence.entity.RefreshTokenJpaEntity;
import com.cuymonitor.backend.domain.model.auth.RefreshToken;
import org.springframework.stereotype.Component;

@Component
public class RefreshTokenPersistenceMapper {

    public RefreshTokenJpaEntity toEntity(RefreshToken token) {
        return new RefreshTokenJpaEntity(token.getId(), token.getUserId(), token.getFamilyId(), token.getTokenHash(),
                token.getCreatedAt(), token.getExpiresAt(), token.getRevokedAt().orElse(null));
    }

    public RefreshToken toDomain(RefreshTokenJpaEntity entity) {
        return RefreshToken.restore(entity.getId(), entity.getUserId(), entity.getFamilyId(), entity.getTokenHash(),
                entity.getCreatedAt(), entity.getExpiresAt(), entity.getRevokedAt());
    }
}
