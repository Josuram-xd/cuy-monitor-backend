package com.cuymonitor.backend.adapter.out.persistence.mapper;

import com.cuymonitor.backend.adapter.out.persistence.entity.CageJpaEntity;
import com.cuymonitor.backend.adapter.out.persistence.entity.WeightReadingJpaEntity;
import com.cuymonitor.backend.domain.model.WeightReading;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
public class WeightReadingPersistenceMapper {
    public WeightReadingJpaEntity toEntity(WeightReading reading, CageJpaEntity cage) {
        return new WeightReadingJpaEntity(
                reading.id(),
                cage,
                BigDecimal.valueOf(reading.grams()),
                reading.stable(),
                reading.measuredAt());
    }

    public WeightReading toDomain(WeightReadingJpaEntity entity) {
        return new WeightReading(
                entity.getId(),
                entity.getCage().getCode(),
                entity.getGrams().doubleValue(),
                entity.isStable(),
                entity.getMeasuredAt());
    }
}
