package com.cuymonitor.backend.domain.health.state;

import com.cuymonitor.backend.domain.model.HealthStatus;
import com.cuymonitor.backend.domain.model.StateTransition;

import java.time.Duration;
import java.time.Instant;
import java.util.Objects;
import java.util.Optional;

/**
 * <b>State pattern — Context.</b> The health of one guinea pig. It holds its current {@link HealthState} and
 * delegates to it every decision about what comes next; it only records the move when the level changes.
 */
public final class GuineaPigHealthContext {
    private static final String RECOVERY_REASON = "Behavior back to its usual range";

    private final long guineaPigId;
    private HealthState state;
    // when it reached the current level: how long it has been there decides whether to go up
    private Instant stateSince;

    public GuineaPigHealthContext(long guineaPigId, HealthState initialState) {
        this(guineaPigId, initialState, Instant.EPOCH);
    }

    public GuineaPigHealthContext(long guineaPigId, HealthState initialState, Instant stateSince) {
        this.guineaPigId = guineaPigId;
        this.state = Objects.requireNonNull(initialState, "initialState");
        this.stateSince = Objects.requireNonNull(stateSince, "stateSince");
    }

    public long guineaPigId() {
        return guineaPigId;
    }

    public HealthState state() {
        return state;
    }

    public HealthStatus status() {
        return state.status();
    }

    /** True when the current level has to warn the breeder (ALERT or CRITICAL). */
    public boolean raisesAlert() {
        return state.raisesAlert();
    }

    /**
     * One more confirmed anomaly. Empty when the level does not change: already CRITICAL, or the anomaly has not
     * lasted long enough at this level for the policy.
     */
    public Optional<StateTransition> anomalyConfirmed(String reason, Instant now, EscalationPolicy policy) {
        Duration timeInState = Duration.between(stateSince, Objects.requireNonNull(now, "now"));
        return moveTo(state.onAnomaly(timeInState, Objects.requireNonNull(policy, "policy")),
                Objects.requireNonNull(reason, "reason"), now);
    }

    /** One more confirmed anomaly with no waiting between levels ({@link EscalationPolicy#immediate()}). */
    public Optional<StateTransition> anomalyConfirmed(String reason, Instant now) {
        return anomalyConfirmed(reason, now, EscalationPolicy.immediate());
    }

    /** The behavior is normal again. Empty when it was already NORMAL. */
    public Optional<StateTransition> recovered(Instant now) {
        return moveTo(state.onRecovery(), RECOVERY_REASON, now);
    }

    void setState(HealthState state) {
        this.state = Objects.requireNonNull(state, "state");
    }

    private Optional<StateTransition> moveTo(HealthState next, String reason, Instant now) {
        HealthStatus from = state.status();
        if (next.status() == from) {
            return Optional.empty();
        }
        setState(next);
        stateSince = Objects.requireNonNull(now, "now");
        return Optional.of(new StateTransition(null, guineaPigId, from, next.status(), reason,
                Objects.requireNonNull(now, "now")));
    }
}
