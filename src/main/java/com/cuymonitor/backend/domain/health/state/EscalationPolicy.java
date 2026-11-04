package com.cuymonitor.backend.domain.health.state;

import java.time.Duration;
import java.util.Objects;

/**
 * How long a sustained anomaly has to last at a level before it goes one level up (Task 10.1). An animal that
 * looks wrong for a few minutes is watched, not reported: OBSERVED only becomes ALERT, and ALERT only becomes
 * CRITICAL, when the anomaly is still confirmed after these times.
 */
public record EscalationPolicy(Duration observedToAlert, Duration alertToCritical) {

    public EscalationPolicy {
        Objects.requireNonNull(observedToAlert, "observedToAlert");
        Objects.requireNonNull(alertToCritical, "alertToCritical");
        if (observedToAlert.isNegative() || alertToCritical.isNegative()) {
            throw new IllegalArgumentException("escalation times can't be negative");
        }
    }

    /** No waiting: every confirmed anomaly goes up a level at once (the behavior before Task 10). */
    public static EscalationPolicy immediate() {
        return new EscalationPolicy(Duration.ZERO, Duration.ZERO);
    }
}
