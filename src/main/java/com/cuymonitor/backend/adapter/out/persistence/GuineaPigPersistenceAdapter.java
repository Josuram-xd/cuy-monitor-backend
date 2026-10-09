package com.cuymonitor.backend.adapter.out.persistence;

import com.cuymonitor.backend.adapter.out.persistence.mapper.GuineaPigPersistenceMapper;
import com.cuymonitor.backend.adapter.out.persistence.repository.CageJpaRepository;
import com.cuymonitor.backend.adapter.out.persistence.repository.GuineaPigJpaRepository;
import com.cuymonitor.backend.domain.model.GuineaPig;
import com.cuymonitor.backend.domain.model.MarkColor;
import com.cuymonitor.backend.domain.port.out.GuineaPigRepository;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Component
@Profile("!memory")
@Transactional
public class GuineaPigPersistenceAdapter implements GuineaPigRepository {

    private final CageJpaRepository cageJpaRepository;
    private final GuineaPigJpaRepository guineaPigJpaRepository;
    private final GuineaPigPersistenceMapper mapper;

    public GuineaPigPersistenceAdapter(CageJpaRepository cageJpaRepository,
                                       GuineaPigJpaRepository guineaPigJpaRepository,
                                       GuineaPigPersistenceMapper mapper) {
        this.cageJpaRepository = cageJpaRepository;
        this.guineaPigJpaRepository = guineaPigJpaRepository;
        this.mapper = mapper;
    }

    @Override
    public GuineaPig save(GuineaPig guineaPig) {
        var cage = PersistenceEntityResolver.requireCage(cageJpaRepository, guineaPig.getCageCode());
        return mapper.toDomain(guineaPigJpaRepository.save(mapper.toEntity(guineaPig, cage)));
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<GuineaPig> findById(long id) {
        return guineaPigJpaRepository.findById(id).map(mapper::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<GuineaPig> findActiveByCageAndColor(String cageCode, MarkColor markColor) {
        return guineaPigJpaRepository.findByCage_CodeAndMarkColorAndActiveTrue(cageCode, markColor)
                .map(mapper::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public List<GuineaPig> findActiveByCage(String cageCode) {
        return guineaPigJpaRepository.findAllByCage_CodeAndActiveTrueOrderByIdAsc(cageCode).stream()
                .map(mapper::toDomain)
                .toList();
    }
}
