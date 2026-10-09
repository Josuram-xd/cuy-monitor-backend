package com.cuymonitor.backend.adapter.out.persistence;

import com.cuymonitor.backend.adapter.out.persistence.mapper.CagePersistenceMapper;
import com.cuymonitor.backend.adapter.out.persistence.repository.CageJpaRepository;
import com.cuymonitor.backend.domain.model.Cage;
import com.cuymonitor.backend.domain.port.out.CageRepository;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Component
@Profile("!memory")
@Transactional(readOnly = true)
public class CagePersistenceAdapter implements CageRepository {

    private final CageJpaRepository cageJpaRepository;
    private final CagePersistenceMapper mapper;

    public CagePersistenceAdapter(CageJpaRepository cageJpaRepository, CagePersistenceMapper mapper) {
        this.cageJpaRepository = cageJpaRepository;
        this.mapper = mapper;
    }

    @Override
    public Optional<Cage> findByCode(String code) {
        return cageJpaRepository.findByCode(code).map(mapper::toDomain);
    }
}
