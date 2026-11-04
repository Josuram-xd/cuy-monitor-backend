package com.cuymonitor.backend.adapter.out.persistence.memory;

import java.util.Comparator;
import java.time.Instant;
import com.cuymonitor.backend.domain.model.StateTransition;
import com.cuymonitor.backend.domain.port.out.StateTransitionRepository;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicLong;

@Component
@Profile("memory")
public class InMemoryStateTransitionRepository implements StateTransitionRepository {

    private final List<StateTransition> transitions = new CopyOnWriteArrayList<>();
    private final AtomicLong sequence = new AtomicLong();

    @Override
    public StateTransition save(StateTransition t) {
        StateTransition stored = new StateTransition(sequence.incrementAndGet(), t.guineaPigId(), t.fromStatus(),
                t.toStatus(), t.reason(), t.occurredAt());
        transitions.add(stored);
        return stored;
    }

    @Override
    public List<StateTransition> findByGuineaPigBetween(long guineaPigId, Instant from, Instant to) {
        return transitions.stream()
                .filter(t -> t.guineaPigId() == guineaPigId && between(t.occurredAt(), from, to))
                .sorted(Comparator.comparing(StateTransition::occurredAt))
                .toList();
    }

    // from <= t <= to, like BETWEEN in SQL
    private static boolean between(Instant t, Instant from, Instant to) {
        return !t.isBefore(from) && !t.isAfter(to);
    }
}
