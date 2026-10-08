package com.cuymonitor.backend.domain.port.out;

import com.cuymonitor.backend.domain.model.EventType;
import com.cuymonitor.backend.domain.model.HealthEvent;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface EventRepository {
    boolean existsById(UUID eventId);
    void save(HealthEvent event, Long guineaPigId);
    // newest first, by occurredAt
    List<HealthEvent> findLatestBehavior(long guineaPigId, int limit);
    Optional<HealthEvent> findLatest(String cageCode, EventType type);
}
