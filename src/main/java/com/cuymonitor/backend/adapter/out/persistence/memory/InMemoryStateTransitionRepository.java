package com.cuymonitor.backend.adapter.out.persistence.memory;

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
}
