package com.cuymonitor.backend.adapter.out.persistence;

import com.cuymonitor.backend.adapter.out.persistence.entity.GuineaPigJpaEntity;
import com.cuymonitor.backend.adapter.out.persistence.mapper.EventPersistenceMapper;
import com.cuymonitor.backend.adapter.out.persistence.repository.CageJpaRepository;
import com.cuymonitor.backend.adapter.out.persistence.repository.EventJpaRepository;
import com.cuymonitor.backend.adapter.out.persistence.repository.GuineaPigJpaRepository;
import com.cuymonitor.backend.domain.model.EventType;
import com.cuymonitor.backend.domain.model.HealthEvent;
import com.cuymonitor.backend.domain.port.out.EventRepository;
import org.springframework.context.annotation.Profile;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
@Profile("!memory")
@Transactional
public class EventPersistenceAdapter implements EventRepository {

    private final CageJpaRepository cageJpaRepository;
    private final EventJpaRepository eventJpaRepository;
    private final GuineaPigJpaRepository guineaPigJpaRepository;
    private final EventPersistenceMapper mapper;

    public EventPersistenceAdapter(CageJpaRepository cageJpaRepository, EventJpaRepository eventJpaRepository,
                                  GuineaPigJpaRepository guineaPigJpaRepository, EventPersistenceMapper mapper) {
        this.cageJpaRepository = cageJpaRepository;
        this.eventJpaRepository = eventJpaRepository;
        this.guineaPigJpaRepository = guineaPigJpaRepository;
        this.mapper = mapper;
    }

    @Override
    @Transactional(readOnly = true)
    public boolean existsById(UUID eventId) {
        return eventJpaRepository.existsById(eventId);
    }

    @Override
    public void save(HealthEvent event, Long guineaPigId) {
        if ((event.type() == EventType.BEHAVIOR) != (guineaPigId != null)) {
            throw new IllegalArgumentException("Only BEHAVIOR events must reference a guinea pig");
        }
        var cage = PersistenceEntityResolver.requireCage(cageJpaRepository, event.cageId());
        GuineaPigJpaEntity guineaPig = guineaPigId == null
                ? null
                : PersistenceEntityResolver.requireGuineaPig(guineaPigJpaRepository, guineaPigId, event.cageId());
        eventJpaRepository.save(mapper.toEntity(event, cage, guineaPig));
    }

    @Override
    @Transactional(readOnly = true)
    public List<HealthEvent> findLatestBehavior(long guineaPigId, int limit) {
        if (limit < 0) {
            throw new IllegalArgumentException("limit must be non-negative");
        }
        if (limit == 0) {
            return List.of();
        }
        return eventJpaRepository.findByGuineaPig_IdAndTypeOrderByOccurredAtDesc(
                        guineaPigId, EventType.BEHAVIOR, PageRequest.of(0, limit))
                .stream()
                .map(mapper::toDomain)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<HealthEvent> findLatest(String cageCode, EventType type) {
        return eventJpaRepository.findFirstByCage_CodeAndTypeOrderByOccurredAtDesc(cageCode, type)
                .map(mapper::toDomain);
    }
}
