package com.cuymonitor.backend.adapter.out.persistence;

import java.util.List;
import java.time.Instant;
import com.cuymonitor.backend.adapter.out.persistence.mapper.StateTransitionPersistenceMapper;
import com.cuymonitor.backend.adapter.out.persistence.repository.GuineaPigJpaRepository;
import com.cuymonitor.backend.adapter.out.persistence.repository.StateTransitionJpaRepository;
import com.cuymonitor.backend.domain.model.StateTransition;
import com.cuymonitor.backend.domain.port.out.StateTransitionRepository;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Profile("!memory")
@Transactional
public class StateTransitionPersistenceAdapter implements StateTransitionRepository {

    private final GuineaPigJpaRepository guineaPigJpaRepository;
    private final StateTransitionJpaRepository stateTransitionJpaRepository;
    private final StateTransitionPersistenceMapper mapper;

    public StateTransitionPersistenceAdapter(GuineaPigJpaRepository guineaPigJpaRepository,
                                             StateTransitionJpaRepository stateTransitionJpaRepository,
                                             StateTransitionPersistenceMapper mapper) {
        this.guineaPigJpaRepository = guineaPigJpaRepository;
        this.stateTransitionJpaRepository = stateTransitionJpaRepository;
        this.mapper = mapper;
    }

    @Override
    public StateTransition save(StateTransition transition) {
        var guineaPig = guineaPigJpaRepository.findById(transition.guineaPigId())
                .orElseThrow(() -> new IllegalArgumentException(
                        "Unknown guinea pig id: " + transition.guineaPigId()));
        return mapper.toDomain(stateTransitionJpaRepository.save(mapper.toEntity(transition, guineaPig)));
    }

    @Override
    @Transactional(readOnly = true)
    public List<StateTransition> findByGuineaPigBetween(long guineaPigId, Instant from, Instant to) {
        return stateTransitionJpaRepository
                .findByGuineaPig_IdAndOccurredAtBetweenOrderByOccurredAtAsc(guineaPigId, from, to).stream()
                .map(mapper::toDomain)
                .toList();
    }
}
