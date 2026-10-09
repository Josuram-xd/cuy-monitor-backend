package com.cuymonitor.backend.adapter.out.persistence;

import com.cuymonitor.backend.adapter.out.persistence.entity.CageJpaEntity;
import com.cuymonitor.backend.adapter.out.persistence.entity.GuineaPigJpaEntity;
import com.cuymonitor.backend.adapter.out.persistence.repository.CageJpaRepository;
import com.cuymonitor.backend.adapter.out.persistence.repository.GuineaPigJpaRepository;

final class PersistenceEntityResolver {

    private PersistenceEntityResolver() {
    }

    static CageJpaEntity requireCage(CageJpaRepository repository, String code) {
        return repository.findByCode(code)
                .orElseThrow(() -> new IllegalArgumentException("Unknown cage code: " + code));
    }

    static GuineaPigJpaEntity requireGuineaPig(GuineaPigJpaRepository repository, long id, String cageCode) {
        GuineaPigJpaEntity guineaPig = repository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Unknown guinea pig id: " + id));
        if (!guineaPig.getCage().getCode().equals(cageCode)) {
            throw new IllegalArgumentException("Guinea pig does not belong to cage: " + cageCode);
        }
        return guineaPig;
    }
}
