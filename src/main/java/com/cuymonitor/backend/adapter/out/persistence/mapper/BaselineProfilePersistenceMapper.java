package com.cuymonitor.backend.adapter.out.persistence.mapper;

import com.cuymonitor.backend.adapter.out.persistence.entity.BaselineProfileJpaEntity;
import com.cuymonitor.backend.adapter.out.persistence.entity.GuineaPigJpaEntity;
import com.cuymonitor.backend.domain.model.BaselineProfile;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
public class BaselineProfilePersistenceMapper {
    public BaselineProfileJpaEntity toEntity(BaselineProfile profile, GuineaPigJpaEntity guineaPig) {
        return new BaselineProfileJpaEntity(
                guineaPig,
                BigDecimal.valueOf(profile.avgStillSeconds()),
                BigDecimal.valueOf(profile.avgFeederVisits()),
                BigDecimal.valueOf(profile.avgGroupDistance()),
                profile.updatedAt());
    }

    public BaselineProfile toDomain(BaselineProfileJpaEntity entity) {
        return new BaselineProfile(
                entity.getGuineaPigId(),
                entity.getAvgStillSeconds().doubleValue(),
                entity.getAvgFeederVisits().doubleValue(),
                entity.getAvgGroupDistance().doubleValue(),
                entity.getUpdatedAt());
    }
}
