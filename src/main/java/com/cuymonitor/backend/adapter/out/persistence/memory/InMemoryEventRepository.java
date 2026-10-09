package com.cuymonitor.backend.adapter.out.persistence.memory;

import com.cuymonitor.backend.domain.model.EventType;
import com.cuymonitor.backend.domain.model.HealthEvent;
import com.cuymonitor.backend.domain.port.out.EventRepository;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class InMemoryEventRepository implements EventRepository {

    private record StoredEvent(HealthEvent event, Long guineaPigId) {
    }

    private static final Comparator<StoredEvent> NEWEST_FIRST =
            Comparator.comparing((StoredEvent s) -> s.event().occurredAt()).reversed();

    private final Map<UUID, StoredEvent> events = new ConcurrentHashMap<>();

    @Override
    public boolean existsById(UUID eventId) {
        return events.containsKey(eventId);
    }

    @Override
    public void save(HealthEvent event, Long guineaPigId) {
        events.putIfAbsent(event.eventId(), new StoredEvent(event, guineaPigId));
    }

    @Override
    public List<HealthEvent> findLatestBehavior(long guineaPigId, int limit) {
        return events.values().stream()
                .filter(s -> s.event().type() == EventType.BEHAVIOR && Objects.equals(s.guineaPigId(), guineaPigId))
                .sorted(NEWEST_FIRST)
                .limit(limit)
                .map(StoredEvent::event)
                .toList();
    }

    @Override
    public Optional<HealthEvent> findLatest(String cageCode, EventType type) {
        return events.values().stream()
                .filter(s -> s.event().type() == type && s.event().cageId().equals(cageCode))
                .sorted(NEWEST_FIRST)
                .map(StoredEvent::event)
                .findFirst();
    }
}
