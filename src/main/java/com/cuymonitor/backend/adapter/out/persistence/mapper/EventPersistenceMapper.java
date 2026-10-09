package com.cuymonitor.backend.adapter.out.persistence.mapper;

import com.cuymonitor.backend.adapter.out.persistence.entity.CageJpaEntity;
import com.cuymonitor.backend.adapter.out.persistence.entity.EventJpaEntity;
import com.cuymonitor.backend.adapter.out.persistence.entity.GuineaPigJpaEntity;
import com.cuymonitor.backend.domain.model.AudioSignal;
import com.cuymonitor.backend.domain.model.BehaviorSignal;
import com.cuymonitor.backend.domain.model.EventType;
import com.cuymonitor.backend.domain.model.HealthEvent;
import com.cuymonitor.backend.domain.model.HealthSignal;
import com.cuymonitor.backend.domain.model.WeightSignal;
import org.springframework.stereotype.Component;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

@Component
public class EventPersistenceMapper {
    private final ObjectMapper objectMapper;

    public EventPersistenceMapper(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public EventJpaEntity toEntity(HealthEvent event, CageJpaEntity cage, GuineaPigJpaEntity guineaPig) {
        JsonNode payload = objectMapper.valueToTree(event.signal());
        return new EventJpaEntity(
                event.eventId(),
                event.type(),
                cage,
                guineaPig,
                event.source(),
                event.occurredAt(),
                payload);
    }

    public HealthEvent toDomain(EventJpaEntity entity) {
        return new HealthEvent(
                entity.getId(),
                entity.getCage().getCode(),
                entity.getOccurredAt(),
                entity.getSource(),
                readSignal(entity.getType(), entity.getPayload()));
    }

    private HealthSignal readSignal(EventType type, JsonNode payload) {
        return switch (type) {
            case BEHAVIOR -> readPayload(payload, BehaviorSignal.class);
            case AUDIO -> readPayload(payload, AudioSignal.class);
            case WEIGHT -> readPayload(payload, WeightSignal.class);
        };
    }

    private <T extends HealthSignal> T readPayload(JsonNode payload, Class<T> targetType) {
        try {
            return objectMapper.treeToValue(payload, targetType);
        } catch (JacksonException exception) {
            throw new IllegalStateException("Stored event payload does not match its event type", exception);
        }
    }
}
