package com.cuymonitor.backend.adapter.in.web.dto;

import com.cuymonitor.backend.domain.model.BaselineProfile;
import com.cuymonitor.backend.domain.model.GuineaPigHistory;
import com.cuymonitor.backend.domain.model.HealthStatus;
import com.cuymonitor.backend.domain.model.StateTransition;

import java.time.Instant;
import java.util.List;

/** Shape of GET /api/v1/guinea-pigs/{id}/history (docs/contracts/rest-api.md, 3.4). */
public record GuineaPigHistoryResponse(long guineaPigId, Instant from, Instant to, List<Transition> transitions,
                                       List<Window> windows, Baseline baseline) {

    public record Transition(HealthStatus fromStatus, HealthStatus toStatus, String reason, Instant occurredAt) {
        static Transition from(StateTransition t) {
            return new Transition(t.fromStatus(), t.toStatus(), t.reason(), t.occurredAt());
        }
    }

    public record Window(Instant occurredAt, double stillSeconds, int feederVisits, int watererVisits,
                         double avgGroupDistance) {
        static Window from(GuineaPigHistory.BehaviorWindow w) {
            return new Window(w.occurredAt(), w.stillSeconds(), w.feederVisits(), w.watererVisits(),
                    w.avgGroupDistance());
        }
    }

    /** What is normal for this guinea pig: the dashboard draws it as the normal range of the chart. */
    public record Baseline(double avgStillSeconds, double avgFeederVisits, double avgGroupDistance) {
        static Baseline from(BaselineProfile b) {
            return new Baseline(b.avgStillSeconds(), b.avgFeederVisits(), b.avgGroupDistance());
        }
    }

    public static GuineaPigHistoryResponse from(GuineaPigHistory history) {
        return new GuineaPigHistoryResponse(history.guineaPigId(), history.range().from(), history.range().to(),
                history.transitions().stream().map(Transition::from).toList(),
                history.windows().stream().map(Window::from).toList(),
                Baseline.from(history.baseline()));
    }
}
