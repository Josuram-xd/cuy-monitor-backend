package com.cuymonitor.backend.adapter.out.persistence;

import com.cuymonitor.backend.adapter.out.persistence.mapper.CagePersistenceMapper;
import com.cuymonitor.backend.adapter.out.persistence.repository.CageJpaRepository;
import com.cuymonitor.backend.domain.model.Cage;
import com.cuymonitor.backend.domain.port.out.CageRepository;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

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
        return cageJpaRepository.findByCodeAndActiveTrue(code).map(mapper::toDomain);
    }

    @Override
    public List<Cage> findActiveByOwner(UUID ownerId) {
        return cageJpaRepository.findByOwnerIdAndActiveTrueOrderByCreatedAtAscIdAsc(ownerId).stream()
                .map(mapper::toDomain)
                .toList();
    }

    @Override
    public boolean existsByCode(String code) {
        return cageJpaRepository.existsByCode(code);
    }

    @Override
    @Transactional
    public Cage save(Cage cage) {
        return mapper.toDomain(cageJpaRepository.save(mapper.toEntity(cage)));
    }
}
