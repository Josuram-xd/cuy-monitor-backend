package com.cuymonitor.backend.adapter.out.persistence;

import com.cuymonitor.backend.adapter.out.persistence.entity.GuineaPigJpaEntity;
import com.cuymonitor.backend.adapter.out.persistence.mapper.GuineaPigPersistenceMapper;
import com.cuymonitor.backend.adapter.out.persistence.repository.CageJpaRepository;
import com.cuymonitor.backend.adapter.out.persistence.repository.GuineaPigJpaRepository;
import com.cuymonitor.backend.domain.model.GuineaPig;
import com.cuymonitor.backend.domain.model.GuineaPigPhoto;
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
    public GuineaPig register(GuineaPig guineaPig, GuineaPigPhoto photo) {
        var cage = PersistenceEntityResolver.requireCage(cageJpaRepository, guineaPig.getCageCode());
        return mapper.toDomain(guineaPigJpaRepository.save(mapper.toEntity(guineaPig, cage, photo)));
    }

    @Override
    public GuineaPig save(GuineaPig guineaPig) {
        var cage = PersistenceEntityResolver.requireCage(cageJpaRepository, guineaPig.getCageCode());
        return mapper.toDomain(guineaPigJpaRepository.save(mapper.toEntity(guineaPig, cage, null)));
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<GuineaPig> findById(long id) {
        return guineaPigJpaRepository.findRowById(id).map(mapper::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<GuineaPig> findActiveByCageAndColor(String cageCode, String markColor) {
        return guineaPigJpaRepository.findActiveRowByCageAndColor(cageCode, markColor).map(mapper::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public List<GuineaPig> findActiveByCage(String cageCode) {
        return guineaPigJpaRepository.findActiveRowsByCage(cageCode).stream()
                .map(mapper::toDomain)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<GuineaPigPhoto> findPhoto(long guineaPigId) {
        return guineaPigJpaRepository.findById(guineaPigId)
                .filter(entity -> entity.getPhoto() != null)
                .map(GuineaPigPersistenceAdapter::toPhoto);
    }

    private static GuineaPigPhoto toPhoto(GuineaPigJpaEntity entity) {
        return new GuineaPigPhoto(entity.getPhoto(), entity.getPhotoContentType());
    }
}
