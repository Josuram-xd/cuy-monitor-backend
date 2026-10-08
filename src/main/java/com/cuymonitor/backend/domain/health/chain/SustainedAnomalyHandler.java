package com.cuymonitor.backend.domain.health.chain;

import com.cuymonitor.backend.domain.model.BaselineProfile;
import com.cuymonitor.backend.domain.model.BehaviorSignal;
import com.cuymonitor.backend.domain.model.HealthEvent;
import com.cuymonitor.backend.domain.port.out.EventRepository;

import java.util.List;

// a single odd window is noise: a change only counts when the last N windows agree,
// both to confirm an anomaly and to confirm that the guinea pig recovered
public class SustainedAnomalyHandler extends EventHandler {

    private final EventRepository eventRepository;
    private final int requiredWindows;

    public SustainedAnomalyHandler(EventRepository eventRepository, int requiredWindows) {
        if (requiredWindows < 1) {
            throw new IllegalArgumentException("requiredWindows must be at least 1");
        }
        this.eventRepository = eventRepository;
        this.requiredWindows = requiredWindows;
    }

    @Override
    protected void process(EventContext context) {
        if (context.guineaPig().isEmpty() || context.baseline().isEmpty()) {
            return;
        }
        HealthEvent current = context.event();
        BaselineProfile baseline = context.baseline().get();
        List<BehaviorSignal> previous = eventRepository
                .findLatestBehavior(context.guineaPig().get().getId(), requiredWindows - 1).stream()
                .filter(e -> e.occurredAt().isBefore(current.occurredAt()))
                .map(e -> (BehaviorSignal) e.signal())
                .toList();
        if (previous.size() < requiredWindows - 1) {
            return;
        }

        if (context.isAnomalous() && previous.stream().allMatch(baseline::isAnomalous)) {
            context.confirmSustainedAnomaly();
        } else if (!context.isAnomalous() && previous.stream().noneMatch(baseline::isAnomalous)) {
            context.confirmSustainedRecovery();
        }
    }
}
