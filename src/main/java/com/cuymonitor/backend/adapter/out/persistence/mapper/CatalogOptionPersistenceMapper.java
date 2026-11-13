package com.cuymonitor.backend.adapter.out.persistence.mapper;

import com.cuymonitor.backend.adapter.out.persistence.entity.CatalogOptionJpaEntity;
import com.cuymonitor.backend.domain.model.CatalogOption;
import org.springframework.stereotype.Component;

@Component
public class CatalogOptionPersistenceMapper {

    public CatalogOption toDomain(CatalogOptionJpaEntity entity) {
        return new CatalogOption(entity.getKind(), entity.getCode(), entity.getLabel(), entity.getHex(),
                entity.getSortOrder(), entity.isActive());
    }

    public CatalogOptionJpaEntity toEntity(CatalogOption option) {
        return new CatalogOptionJpaEntity(option.kind(), option.code(), option.label(), option.hex(),
                (short) option.sortOrder(), option.active());
    }
}
