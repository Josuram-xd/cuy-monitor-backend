package com.cuymonitor.backend.domain.health.chain;

import com.cuymonitor.backend.domain.model.BehaviorSignal;
import com.cuymonitor.backend.domain.port.out.EventRepository;

import java.util.Objects;

public final class SustainedAnomalyHandler extends EventHandler {

    private final EventRepository eventRepository;
    private final int requiredConsecutiveWindows;

    public SustainedAnomalyHandler(EventRepository eventRepository, int requiredConsecutiveWindows) {
        this.eventRepository = Objects.requireNonNull(eventRepository, "eventRepository");
        if (requiredConsecutiveWindows < 1) {
            throw new IllegalArgumentException("requiredConsecutiveWindows must be positive");
        }
        this.requiredConsecutiveWindows = requiredConsecutiveWindows;
    }

    @Override
    protected void handleCurrent(EventHandlerContext context) {
        if (context.anomalyReason().isEmpty()) {
            return;
        }

        var guineaPig = context.guineaPig().orElse(null);
        var baseline = context.baselineProfile().orElse(null);
        if (guineaPig == null || guineaPig.getId() == null || baseline == null) {
            context.reject("Anomaly evaluation requires an identified guinea pig and baseline");
            return;
        }
        if (requiredConsecutiveWindows == 1) {
            context.markAnomaly("Anomaly in 1 consecutive window");
            return;
        }

        int consecutiveAnomalies = 1;
        var previousEvents = eventRepository.findLatestBehavior(
                guineaPig.getId(),
                requiredConsecutiveWindows
        );
        for (var previousEvent : previousEvents) {
            if (previousEvent.eventId().equals(context.event().eventId())) {
                continue;
            }
            if (!(previousEvent.signal() instanceof BehaviorSignal behavior)
                    || !baseline.isAnomalous(behavior)) {
                break;
            }
            consecutiveAnomalies++;
            if (consecutiveAnomalies >= requiredConsecutiveWindows) {
                context.markAnomaly(
                        "Anomaly in " + requiredConsecutiveWindows + " consecutive windows"
                );
                return;
            }
        }

        context.clearAnomaly();
    }
}
