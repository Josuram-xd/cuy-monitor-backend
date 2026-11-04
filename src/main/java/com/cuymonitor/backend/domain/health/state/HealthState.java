package com.cuymonitor.backend.domain.health.state;

import com.cuymonitor.backend.domain.model.HealthStatus;

/**
 * <b>State pattern — State.</b> One health level of a guinea pig. Each state decides on its own what comes
 * next, so the context never asks "which status am I in?":
 *
 * <pre>
 * NORMAL ──anomaly──► OBSERVED ──anomaly──► ALERT ──anomaly──► CRITICAL
 *    ▲                   │                    │                    │
 *    └──── recovery ─────┴──── recovery ──────┴──── recovery ──────┘
 * </pre>
 *
 * An anomaly here is one already confirmed by the chain (sustained over N windows), never a single window.
 */
public interface HealthState {

    HealthStatus status();

    /** The state after one more confirmed anomaly. */
    HealthState onAnomaly();

    /** The state once the behavior is back to normal: every level goes straight back to NORMAL. */
    default HealthState onRecovery() {
        return new NormalState();
    }

    /** Whether reaching this state has to warn the breeder. */
    default boolean raisesAlert() {
        return false;
    }
}
