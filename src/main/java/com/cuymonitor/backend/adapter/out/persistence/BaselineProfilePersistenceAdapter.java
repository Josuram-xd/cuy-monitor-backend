package com.cuymonitor.backend.adapter.out.persistence;

import com.cuymonitor.backend.adapter.out.persistence.repository.GuineaPigJpaRepository;
import com.cuymonitor.backend.adapter.out.persistence.mapper.BaselineProfilePersistenceMapper;
import com.cuymonitor.backend.adapter.out.persistence.repository.BaselineProfileJpaRepository;
import com.cuymonitor.backend.domain.model.BaselineProfile;
import com.cuymonitor.backend.domain.port.out.BaselineProfileRepository;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Component
@Profile("!memory")
@Transactional(readOnly = true)
public class BaselineProfilePersistenceAdapter implements BaselineProfileRepository {

    private final BaselineProfileJpaRepository baselineProfileJpaRepository;
    private final GuineaPigJpaRepository guineaPigJpaRepository;
    private final BaselineProfilePersistenceMapper mapper;

    public BaselineProfilePersistenceAdapter(BaselineProfileJpaRepository baselineProfileJpaRepository,
                                             GuineaPigJpaRepository guineaPigJpaRepository,
                                             BaselineProfilePersistenceMapper mapper) {
        this.baselineProfileJpaRepository = baselineProfileJpaRepository;
        this.guineaPigJpaRepository = guineaPigJpaRepository;
        this.mapper = mapper;
    }

    @Override
    public Optional<BaselineProfile> findByGuineaPigId(long guineaPigId) {
        return baselineProfileJpaRepository.findByGuineaPig_Id(guineaPigId).map(mapper::toDomain);
    }

    @Override
    @Transactional
    public BaselineProfile save(BaselineProfile profile) {
        var existing = baselineProfileJpaRepository.findByGuineaPig_Id(profile.guineaPigId());
        if (existing.isPresent()) {
            // managed entity: the change is written when the transaction commits
            mapper.copyInto(profile, existing.get());
            return mapper.toDomain(existing.get());
        }
        var guineaPig = guineaPigJpaRepository.findById(profile.guineaPigId())
                .orElseThrow(() -> new IllegalArgumentException("Unknown guinea pig id: " + profile.guineaPigId()));
        return mapper.toDomain(baselineProfileJpaRepository.save(mapper.toEntity(profile, guineaPig)));
    }
}
