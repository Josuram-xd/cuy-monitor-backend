package com.cuymonitor.backend.adapter.out.persistence;

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
    private final BaselineProfilePersistenceMapper mapper;

    public BaselineProfilePersistenceAdapter(BaselineProfileJpaRepository baselineProfileJpaRepository,
                                             BaselineProfilePersistenceMapper mapper) {
        this.baselineProfileJpaRepository = baselineProfileJpaRepository;
        this.mapper = mapper;
    }

    @Override
    public Optional<BaselineProfile> findByGuineaPigId(long guineaPigId) {
        return baselineProfileJpaRepository.findByGuineaPig_Id(guineaPigId).map(mapper::toDomain);
    }
}
