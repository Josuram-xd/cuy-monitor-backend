package com.cuymonitor.backend.adapter.out.persistence;

import com.cuymonitor.backend.adapter.out.persistence.mapper.WeightReadingPersistenceMapper;
import com.cuymonitor.backend.adapter.out.persistence.repository.CageJpaRepository;
import com.cuymonitor.backend.adapter.out.persistence.repository.WeightReadingJpaRepository;
import com.cuymonitor.backend.domain.model.WeightReading;
import com.cuymonitor.backend.domain.port.out.WeightReadingRepository;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Component
@Profile("!memory")
@Transactional
public class WeightReadingPersistenceAdapter implements WeightReadingRepository {

    private final CageJpaRepository cageJpaRepository;
    private final WeightReadingJpaRepository weightReadingJpaRepository;
    private final WeightReadingPersistenceMapper mapper;

    public WeightReadingPersistenceAdapter(CageJpaRepository cageJpaRepository,
                                           WeightReadingJpaRepository weightReadingJpaRepository,
                                           WeightReadingPersistenceMapper mapper) {
        this.cageJpaRepository = cageJpaRepository;
        this.weightReadingJpaRepository = weightReadingJpaRepository;
        this.mapper = mapper;
    }

    @Override
    public WeightReading save(WeightReading reading) {
        var cage = PersistenceEntityResolver.requireCage(cageJpaRepository, reading.cageCode());
        return mapper.toDomain(weightReadingJpaRepository.save(mapper.toEntity(reading, cage)));
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<WeightReading> findLatest(String cageCode) {
        return weightReadingJpaRepository.findFirstByCage_CodeOrderByMeasuredAtDesc(cageCode)
                .map(mapper::toDomain);
    }
}
