package com.cuymonitor.backend.domain.health.chain;

import com.cuymonitor.backend.domain.model.AudioSignal;
import com.cuymonitor.backend.domain.model.BehaviorSignal;
import com.cuymonitor.backend.domain.model.WeightSignal;

import java.time.Clock;
import java.time.Duration;
import java.util.Objects;

public final class ValidationHandler extends EventHandler {

    private final Clock clock;
    private final Duration maximumEventAge;
    private final double minimumDetectionConfidence;

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
        this.minimumDetectionConfidence = minimumDetectionConfidence;
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

        if (event.signal() instanceof BehaviorSignal behavior
                && (!hasFiniteMeasurements(behavior)
                || behavior.detectionConfidence() < minimumDetectionConfidence)) {
            context.reject("Behavior event has invalid measurements or low detection confidence");
            return;
        }
        if (event.signal() instanceof AudioSignal audio && !Double.isFinite(audio.probability())) {
            context.reject("Audio event has invalid measurements");
            return;
        }
        if (event.signal() instanceof WeightSignal weight && !Double.isFinite(weight.grams())) {
            context.reject("Weight event has invalid measurements");
        }
    }

    private static boolean hasFiniteMeasurements(BehaviorSignal behavior) {
        return Double.isFinite(behavior.stillSeconds())
                && Double.isFinite(behavior.avgGroupDistance())
                && Double.isFinite(behavior.probAnomaly())
                && Double.isFinite(behavior.detectionConfidence());
    }
}
