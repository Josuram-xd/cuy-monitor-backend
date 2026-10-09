package com.cuymonitor.backend.adapter.out.persistence.mapper;

import com.cuymonitor.backend.adapter.out.persistence.entity.CageJpaEntity;
import com.cuymonitor.backend.adapter.out.persistence.entity.GuineaPigJpaEntity;
import com.cuymonitor.backend.domain.model.GuineaPig;
import org.springframework.stereotype.Component;

@Component
public class GuineaPigPersistenceMapper {
    public GuineaPigJpaEntity toEntity(GuineaPig guineaPig, CageJpaEntity cage) {
        return new GuineaPigJpaEntity(
                guineaPig.getId(),
                cage,
                guineaPig.getName(),
                guineaPig.getMarkColor(),
                guineaPig.getStatus(),
                guineaPig.getStatusSince(),
                guineaPig.isActive(),
                guineaPig.getCreatedAt());
    }

    public GuineaPig toDomain(GuineaPigJpaEntity entity) {
        return GuineaPig.restore(
                entity.getId(),
                entity.getCage().getCode(),
                entity.getName(),
                entity.getMarkColor(),
                entity.getStatus(),
                entity.getStatusSince(),
                entity.isActive(),
                entity.getCreatedAt());
    }
}
