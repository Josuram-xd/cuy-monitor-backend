package com.cuymonitor.backend.domain.health.state;

import com.cuymonitor.backend.domain.model.HealthStatus;

import java.time.Duration;

/**
 * <b>State pattern — State.</b> One health level of a guinea pig. Each state decides on its own what comes
 * next, so the context never asks "which status am I in?":
 *
 * <pre>
 * NORMAL ─anomaly─► OBSERVED ─anomaly (t ≥ observedToAlert)─► ALERT ─anomaly (t ≥ alertToCritical)─► CRITICAL
 *    ▲                 │                                         │                                       │
 *    └─── recovery ────┴──────────────── recovery ───────────────┴──────────────── recovery ─────────────┘
 * </pre>
 *
 * An anomaly here is one already confirmed by the chain (sustained over N windows), never a single window, and
 * a recovery is one confirmed over M normal windows in a row (Task 10.2). How long the guinea pig has been at the
 * current level decides whether it is time to go up (Task 10.1, {@link EscalationPolicy}).
 */
public interface HealthState {

    HealthStatus status();

    /**
     * The state after one more confirmed anomaly.
     *
     * @param timeInState how long the guinea pig has been at this level
     */
    HealthState onAnomaly(Duration timeInState, EscalationPolicy policy);

    /** The state once the behavior is back to normal: every level goes straight back to NORMAL. */
    default HealthState onRecovery() {
        return new NormalState();
    }

    /** Whether reaching this state has to warn the breeder. */
    default boolean raisesAlert() {
        return false;
    }
}
