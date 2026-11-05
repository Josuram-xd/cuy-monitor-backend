package com.cuymonitor.backend.domain.port.out;

import java.util.List;
import java.time.Instant;
import com.cuymonitor.backend.domain.model.StateTransition;

public interface StateTransitionRepository {
    StateTransition save(StateTransition transition);

    /** The moves of one guinea pig with from <= occurredAt <= to, oldest first. */
    List<StateTransition> findByGuineaPigBetween(long guineaPigId, Instant from, Instant to);
}
