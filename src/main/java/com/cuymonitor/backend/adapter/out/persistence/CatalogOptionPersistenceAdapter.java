package com.cuymonitor.backend.adapter.out.persistence;

import com.cuymonitor.backend.adapter.out.persistence.mapper.CatalogOptionPersistenceMapper;
import com.cuymonitor.backend.adapter.out.persistence.repository.CatalogOptionJpaRepository;
import com.cuymonitor.backend.domain.model.CatalogKind;
import com.cuymonitor.backend.domain.model.CatalogOption;
import com.cuymonitor.backend.domain.port.out.CatalogOptionRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Component
@Transactional(readOnly = true)
public class CatalogOptionPersistenceAdapter implements CatalogOptionRepository {

    private final CatalogOptionJpaRepository catalogOptionJpaRepository;
    private final CatalogOptionPersistenceMapper mapper;

    public CatalogOptionPersistenceAdapter(CatalogOptionJpaRepository catalogOptionJpaRepository,
                                           CatalogOptionPersistenceMapper mapper) {
        this.catalogOptionJpaRepository = catalogOptionJpaRepository;
        this.mapper = mapper;
    }

    @Override
    public List<CatalogOption> findAllActive() {
        return catalogOptionJpaRepository.findAllByActiveTrueOrderByKindAscSortOrderAsc().stream()
                .map(mapper::toDomain)
                .toList();
    }

    @Override
    public List<CatalogOption> findAll() {
        return catalogOptionJpaRepository.findAllByOrderByKindAscSortOrderAsc().stream()
                .map(mapper::toDomain)
                .toList();
    }

    @Override
    public Optional<CatalogOption> find(CatalogKind kind, String code) {
        String normalized = CatalogOption.normalize(code);
        return normalized == null
                ? Optional.empty()
                : catalogOptionJpaRepository.findByKindAndCode(kind, normalized).map(mapper::toDomain);
    }

    @Override
    @Transactional
    public CatalogOption save(CatalogOption option) {
        return mapper.toDomain(catalogOptionJpaRepository.save(mapper.toEntity(option)));
    }

    @Override
    public boolean existsActive(CatalogKind kind, String code) {
        String normalized = CatalogOption.normalize(code);
        return normalized != null && catalogOptionJpaRepository.existsByKindAndCodeAndActiveTrue(kind, normalized);
    }
}
