package com.cuymonitor.backend.domain.health.state;

import com.cuymonitor.backend.domain.model.GuineaPig;
import com.cuymonitor.backend.domain.model.HealthStatus;
import com.cuymonitor.backend.domain.model.StateTransition;
import com.cuymonitor.backend.domain.port.out.StateTransitionRepository;

import java.time.Clock;
import java.time.Instant;
import java.util.Objects;
import java.util.Optional;

public class GuineaPigHealthContext {

    private final GuineaPig guineaPig;
    private final StateTransitionRepository transitionRepository;
    private final Clock clock;
    private HealthState state;

    public GuineaPigHealthContext(GuineaPig guineaPig, StateTransitionRepository transitionRepository, Clock clock) {
        this.guineaPig = Objects.requireNonNull(guineaPig, "guineaPig");
        this.transitionRepository = transitionRepository;
        this.clock = clock;
        this.state = stateFor(guineaPig.getStatus());
    }

    public HealthStatus status() {
        return state.status();
    }

    public Optional<StateTransition> onSustainedAnomaly(String reason) {
        return moveTo(state.onSustainedAnomaly(), reason);
    }

    public Optional<StateTransition> onSustainedRecovery(String reason) {
        return moveTo(state.onSustainedRecovery(), reason);
    }

    private Optional<StateTransition> moveTo(HealthState next, String reason) {
        HealthStatus from = state.status();
        HealthStatus to = next.status();
        if (to == from) {
            return Optional.empty();
        }
        Instant now = clock.instant();
        state = next;
        guineaPig.changeStatus(to, now);
        return Optional.of(transitionRepository.save(
                new StateTransition(null, guineaPig.getId(), from, to, reason, now)));
    }

    // only rebuilds the stored status; the transitions themselves are decided by each state
    private static HealthState stateFor(HealthStatus status) {
        return switch (status) {
            case NORMAL -> new NormalState();
            case OBSERVED -> new ObservedState();
            case ALERT -> new AlertState();
            case CRITICAL -> new CriticalState();
        };
    }
}
