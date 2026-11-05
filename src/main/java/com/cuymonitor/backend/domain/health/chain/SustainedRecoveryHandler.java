package com.cuymonitor.backend.domain.health.chain;

import com.cuymonitor.backend.domain.model.BehaviorSignal;
import com.cuymonitor.backend.domain.port.out.EventRepository;

import java.util.Objects;

/**
 * Last link of the chain (Task 10.2): a guinea pig only counts as recovered when its last M windows, this one
 * included, were all normal. One good minute is not enough to say an animal is well again. Runs only for a
 * normal window of an identified guinea pig; an anomaly is SustainedAnomalyHandler's job.
 */
public final class SustainedRecoveryHandler extends EventHandler {
    private final EventRepository eventRepository;
    private final int requiredNormalWindows;

    public SustainedRecoveryHandler(EventRepository eventRepository, int requiredNormalWindows) {
        this.eventRepository = Objects.requireNonNull(eventRepository, "eventRepository");
        if (requiredNormalWindows < 1) {
            throw new IllegalArgumentException("requiredNormalWindows must be positive");
        }
        this.requiredNormalWindows = requiredNormalWindows;
    }

    @Override
    protected void handleCurrent(EventHandlerContext context) {
        if (context.anomalyReason().isPresent() || !(context.event().signal() instanceof BehaviorSignal)) {
            return;
        }
        var guineaPig = context.guineaPig().orElse(null);
        var baseline = context.baselineProfile().orElse(null);
        if (guineaPig == null || guineaPig.getId() == null || baseline == null) {
            return;
        }
        // the current window is already stored, so it is the first of the latest ones
        var latest = eventRepository.findLatestBehavior(guineaPig.getId(), requiredNormalWindows);
        boolean allNormal = latest.size() >= requiredNormalWindows && latest.stream()
                .allMatch(event -> event.signal() instanceof BehaviorSignal window && !baseline.isAnomalous(window));
        if (allNormal) {
            context.confirmRecovery();
        }
    }
}
