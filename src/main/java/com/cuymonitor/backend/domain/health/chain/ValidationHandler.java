package com.cuymonitor.backend.domain.health.chain;

import com.cuymonitor.backend.domain.model.BehaviorSignal;
import com.cuymonitor.backend.domain.model.HealthEvent;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;

public class ValidationHandler extends EventHandler {

    // producer clocks are not perfectly in sync with the server
    private static final Duration CLOCK_SKEW = Duration.ofMinutes(5);

    private final Clock clock;
    private final Duration maxEventAge;
    private final double minDetectionConfidence;

    public ValidationHandler(Clock clock, Duration maxEventAge, double minDetectionConfidence) {
        this.clock = clock;
        this.maxEventAge = maxEventAge;
        this.minDetectionConfidence = minDetectionConfidence;
    }

    @Override
    protected void process(EventContext context) {
        HealthEvent event = context.event();
        Instant now = clock.instant();

        if (event.occurredAt().isAfter(now.plus(CLOCK_SKEW))) {
            context.drop("timestamp in the future");
        } else if (event.occurredAt().isBefore(now.minus(maxEventAge))) {
            context.drop("stale event");
        } else if (event.signal() instanceof BehaviorSignal window
                && window.detectionConfidence() < minDetectionConfidence) {
            context.drop("low detection confidence");
        }
    }
}
