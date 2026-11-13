package com.cuymonitor.backend.adapter.out.persistence.mapper;

import com.cuymonitor.backend.adapter.out.persistence.entity.AlertJpaEntity;
import com.cuymonitor.backend.adapter.out.persistence.entity.CageJpaEntity;
import com.cuymonitor.backend.adapter.out.persistence.entity.GuineaPigJpaEntity;
import com.cuymonitor.backend.domain.model.Alert;
import org.springframework.stereotype.Component;

@Component
public class AlertPersistenceMapper {
    public AlertJpaEntity toEntity(Alert alert, CageJpaEntity cage, GuineaPigJpaEntity guineaPig) {
        var entity = new AlertJpaEntity(
                alert.getId(),
                cage,
                guineaPig,
                alert.getLevel(),
                alert.getType(),
                alert.getMessage(),
                alert.getStatus(),
                alert.getCreatedAt(),
                alert.getReviewedAt());
        entity.setAiNote(alert.getAiNote(), alert.getAiNoteAt());
        return entity;
    }

    public Alert toDomain(AlertJpaEntity entity) {
        Long guineaPigId = entity.getGuineaPig() == null ? null : entity.getGuineaPig().getId();
        Alert alert = Alert.restore(
                entity.getId(),
                entity.getCage().getCode(),
                guineaPigId,
                entity.getLevel(),
                entity.getType(),
                entity.getMessage(),
                entity.getStatus(),
                entity.getCreatedAt(),
                entity.getReviewedAt());
        if (entity.getAiNote() != null) {
            alert.attachAiNote(entity.getAiNote(), entity.getAiNoteAt());
        }
        return alert;
    }
}
