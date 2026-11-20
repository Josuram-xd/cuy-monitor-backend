package com.cuymonitor.backend.domain.health.chain;

import java.time.Clock;
import java.time.Duration;
import java.util.Objects;

public final class ValidationHandler extends EventHandler {

    private final Clock clock;
    private final Duration maximumEventAge;
    private final MeasurementCheck measurements;

    public ValidationHandler(Clock clock, Duration maximumEventAge, double minimumDetectionConfidence) {
        this.clock = Objects.requireNonNull(clock, "clock");
        this.maximumEventAge = Objects.requireNonNull(maximumEventAge, "maximumEventAge");
        if (maximumEventAge.isZero() || maximumEventAge.isNegative()) {
            throw new IllegalArgumentException("maximumEventAge must be positive");
        }
        if (!Double.isFinite(minimumDetectionConfidence)
                || minimumDetectionConfidence < 0
                || minimumDetectionConfidence > 1) {
            throw new IllegalArgumentException("minimumDetectionConfidence must be between 0 and 1");
        }
        this.measurements = new MeasurementCheck(minimumDetectionConfidence);
    }

    @Override
    protected void handleCurrent(EventHandlerContext context) {
        var event = context.event();
        if (event.cageId().isBlank() || event.source().isBlank()) {
            context.reject("Event cageId and source must not be blank");
            return;
        }

        var now = clock.instant();
        if (event.occurredAt().isAfter(now)) {
            context.reject("Event timestamp must not be in the future");
            return;
        }
        if (event.occurredAt().isBefore(now.minus(maximumEventAge))) {
            context.reject("Event is stale");
            return;
        }

        event.signal().accept(measurements).ifPresent(context::reject);
    }
}
