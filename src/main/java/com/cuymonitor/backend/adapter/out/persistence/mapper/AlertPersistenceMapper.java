package com.cuymonitor.backend.adapter.out.persistence.mapper;

import com.cuymonitor.backend.adapter.out.persistence.entity.AlertJpaEntity;
import com.cuymonitor.backend.adapter.out.persistence.entity.CageJpaEntity;
import com.cuymonitor.backend.adapter.out.persistence.entity.GuineaPigJpaEntity;
import com.cuymonitor.backend.domain.model.Alert;
import org.springframework.stereotype.Component;

@Component
public class AlertPersistenceMapper {
    public AlertJpaEntity toEntity(Alert alert, CageJpaEntity cage, GuineaPigJpaEntity guineaPig) {
        return new AlertJpaEntity(
                alert.getId(),
                cage,
                guineaPig,
                alert.getLevel(),
                alert.getType(),
                alert.getMessage(),
                alert.getStatus(),
                alert.getCreatedAt(),
                alert.getReviewedAt());
    }

    public Alert toDomain(AlertJpaEntity entity) {
        Long guineaPigId = entity.getGuineaPig() == null ? null : entity.getGuineaPig().getId();
        return Alert.restore(
                entity.getId(),
                entity.getCage().getCode(),
                guineaPigId,
                entity.getLevel(),
                entity.getType(),
                entity.getMessage(),
                entity.getStatus(),
                entity.getCreatedAt(),
                entity.getReviewedAt());
    }
}
