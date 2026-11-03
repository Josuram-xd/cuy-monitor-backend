package com.cuymonitor.backend.adapter.out.persistence.mapper;

import com.cuymonitor.backend.adapter.out.persistence.entity.CageJpaEntity;
import com.cuymonitor.backend.domain.model.Cage;
import org.springframework.stereotype.Component;

@Component
public class CagePersistenceMapper {
    public CageJpaEntity toEntity(Cage cage) {
        return new CageJpaEntity(cage.id(), cage.code(), cage.name(), cage.location(), cage.ownerId(), cage.active(),
                cage.createdAt());
    }

    public Cage toDomain(CageJpaEntity entity) {
        return new Cage(entity.getId(), entity.getCode(), entity.getName(), entity.getLocation(), entity.getOwnerId(),
                entity.isActive(), entity.getCreatedAt());
    }
}
