package com.cuymonitor.backend.adapter.out.persistence.mapper;

import com.cuymonitor.backend.adapter.out.persistence.entity.GuineaPigJpaEntity;
import com.cuymonitor.backend.adapter.out.persistence.entity.StateTransitionJpaEntity;
import com.cuymonitor.backend.domain.model.StateTransition;
import org.springframework.stereotype.Component;

@Component
public class StateTransitionPersistenceMapper {
    public StateTransitionJpaEntity toEntity(StateTransition transition, GuineaPigJpaEntity guineaPig) {
        return new StateTransitionJpaEntity(
                transition.id(),
                guineaPig,
                transition.fromStatus(),
                transition.toStatus(),
                transition.reason(),
                transition.occurredAt());
    }

    public StateTransition toDomain(StateTransitionJpaEntity entity) {
        return new StateTransition(
                entity.getId(),
                entity.getGuineaPig().getId(),
                entity.getFromStatus(),
                entity.getToStatus(),
                entity.getReason(),
                entity.getOccurredAt());
    }
}
