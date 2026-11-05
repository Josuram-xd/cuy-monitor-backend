package com.cuymonitor.backend.adapter.out.persistence.mapper;

import com.cuymonitor.backend.adapter.out.persistence.entity.BaselineProfileJpaEntity;
import com.cuymonitor.backend.adapter.out.persistence.entity.GuineaPigJpaEntity;
import com.cuymonitor.backend.domain.model.BaselineProfile;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Component
public class BaselineProfilePersistenceMapper {
    public BaselineProfileJpaEntity toEntity(BaselineProfile profile, GuineaPigJpaEntity guineaPig) {
        return new BaselineProfileJpaEntity(guineaPig, seconds(profile.avgStillSeconds()),
                seconds(profile.avgFeederVisits()), ratio(profile.avgGroupDistance()), profile.updatedAt(),
                profile.sampleCount());
    }

    public void copyInto(BaselineProfile profile, BaselineProfileJpaEntity entity) {
        entity.relearn(seconds(profile.avgStillSeconds()), seconds(profile.avgFeederVisits()),
                ratio(profile.avgGroupDistance()), profile.updatedAt(), profile.sampleCount());
    }

    // the scale of the columns: NUMERIC(6,2) and NUMERIC(5,4)
    private static BigDecimal seconds(double value) {
        return BigDecimal.valueOf(value).setScale(2, RoundingMode.HALF_UP);
    }

    private static BigDecimal ratio(double value) {
        return BigDecimal.valueOf(value).setScale(4, RoundingMode.HALF_UP);
    }

    public BaselineProfile toDomain(BaselineProfileJpaEntity entity) {
        return new BaselineProfile(
                entity.getGuineaPig().getId(),
                entity.getAvgStillSeconds().doubleValue(),
                entity.getAvgFeederVisits().doubleValue(),
                entity.getAvgGroupDistance().doubleValue(),
                entity.getUpdatedAt(),
                entity.getSampleCount());
    }
}
