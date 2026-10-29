package com.cuymonitor.backend.adapter.out.persistence.mapper;

import com.cuymonitor.backend.adapter.out.persistence.GuineaPigRow;
import com.cuymonitor.backend.adapter.out.persistence.entity.CageJpaEntity;
import com.cuymonitor.backend.adapter.out.persistence.entity.GuineaPigJpaEntity;
import com.cuymonitor.backend.domain.model.GuineaPig;
import com.cuymonitor.backend.domain.model.GuineaPigPhoto;
import com.cuymonitor.backend.domain.model.GuineaPigProfile;
import org.springframework.stereotype.Component;

@Component
public class GuineaPigPersistenceMapper {

    /** photo is null for a save that only changes the state: the column is not updatable. */
    public GuineaPigJpaEntity toEntity(GuineaPig guineaPig, CageJpaEntity cage, GuineaPigPhoto photo) {
        return new GuineaPigJpaEntity(
                guineaPig.getId(),
                cage,
                photo == null ? null : photo.content(),
                guineaPig.getPhotoContentType(),
                guineaPig.getMarkColor(),
                guineaPig.getStatus(),
                guineaPig.getStatusSince(),
                guineaPig.isActive(),
                guineaPig.getCreatedAt(),
                guineaPig.getProfile().breed(),
                guineaPig.getProfile().coatColor(),
                guineaPig.getProfile().initialWeightGrams(),
                guineaPig.getProfile().notes());
    }

    public GuineaPig toDomain(GuineaPigRow row) {
        return GuineaPig.restore(
                row.id(),
                row.cageCode(),
                row.photoContentType(),
                row.markColor(),
                row.status(),
                row.statusSince(),
                row.active(),
                row.createdAt(),
                new GuineaPigProfile(row.breed(), row.coatColor(), row.initialWeightGrams(), row.notes()));
    }

    public GuineaPig toDomain(GuineaPigJpaEntity entity) {
        return GuineaPig.restore(
                entity.getId(),
                entity.getCage().getCode(),
                entity.getPhotoContentType(),
                entity.getMarkColor(),
                entity.getStatus(),
                entity.getStatusSince(),
                entity.isActive(),
                entity.getCreatedAt(),
                new GuineaPigProfile(entity.getBreed(), entity.getCoatColor(), entity.getInitialWeightGrams(),
                        entity.getNotes()));
    }
}
