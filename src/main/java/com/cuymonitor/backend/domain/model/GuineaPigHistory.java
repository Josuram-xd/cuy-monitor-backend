package com.cuymonitor.backend.domain.model;

import java.time.Instant;
import java.util.List;
import java.util.Objects;

/**
 * What happened to one guinea pig in a time range: its state changes and its behavior windows (oldest first),
 * and the baseline those windows are compared with, so the dashboard can draw the normal range.
 */
public record GuineaPigHistory(long guineaPigId, TimeRange range, List<StateTransition> transitions,
                               List<BehaviorWindow> windows, BaselineProfile baseline) {

    public GuineaPigHistory {
        Objects.requireNonNull(range, "range");
        transitions = List.copyOf(transitions);
        windows = List.copyOf(windows);
        Objects.requireNonNull(baseline, "baseline");
    }

    /** One stored BEHAVIOR window, as the chart needs it. */
    public record BehaviorWindow(Instant occurredAt, double stillSeconds, int feederVisits,
                                 int watererVisits, double avgGroupDistance) {

        public static BehaviorWindow of(HealthEvent event) {
            if (!(event.signal() instanceof BehaviorSignal behavior)) {
                throw new IllegalArgumentException("not a BEHAVIOR event: " + event.eventId());
            }
            return new BehaviorWindow(event.occurredAt(), behavior.stillSeconds(), behavior.feederVisits(),
                    behavior.watererVisits(), behavior.avgGroupDistance());
        }
    }
}
