package com.cuymonitor.backend.domain.health.state;

import com.cuymonitor.backend.domain.model.Alert;
import com.cuymonitor.backend.domain.model.GuineaPig;
import com.cuymonitor.backend.domain.model.HealthStatus;
import com.cuymonitor.backend.domain.model.StateTransition;
import com.cuymonitor.backend.domain.notification.AlertPublisher;
import com.cuymonitor.backend.domain.port.out.StateTransitionRepository;

import java.time.Clock;
import java.time.Instant;
import java.util.Objects;
import java.util.Optional;

public class GuineaPigHealthContext {

    private final GuineaPig guineaPig;
    private final StateTransitionRepository transitionRepository;
    private final AlertPublisher alertPublisher;
    private final Clock clock;
    private HealthState state;

    public GuineaPigHealthContext(GuineaPig guineaPig, StateTransitionRepository transitionRepository,
                                  AlertPublisher alertPublisher, Clock clock) {
        this.guineaPig = Objects.requireNonNull(guineaPig, "guineaPig");
        this.transitionRepository = transitionRepository;
        this.alertPublisher = alertPublisher;
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
        StateTransition transition = transitionRepository.save(
                new StateTransition(null, guineaPig.getId(), from, to, reason, now));

        // only going up to ALERT or CRITICAL warns the farmer; getting better never does
        if (to.raisesAlert() && to.isWorseThan(from)) {
            alertPublisher.publish(Alert.forGuineaPig(guineaPig, to, alertMessage(to, reason), now));
        }
        return Optional.of(transition);
    }

    private String alertMessage(HealthStatus level, String reason) {
        String label = level == HealthStatus.CRITICAL ? "en estado crítico" : "en alerta";
        return guineaPig.getName() + " está " + label + ": " + reason;
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
