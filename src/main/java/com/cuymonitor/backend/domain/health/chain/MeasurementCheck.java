package com.cuymonitor.backend.domain.health.chain;

import com.cuymonitor.backend.domain.model.AudioSignal;
import com.cuymonitor.backend.domain.model.BehaviorSignal;
import com.cuymonitor.backend.domain.model.HealthSignalVisitor;
import com.cuymonitor.backend.domain.model.WeightSignal;

import java.util.Optional;

/**
 * <b>Visitor.</b> The reason to reject a signal whose measurements can't be used, or nothing when they are fine.
 * One rule per signal type, so the validation never needs an {@code instanceof} per type.
 */
final class MeasurementCheck implements HealthSignalVisitor<Optional<String>> {

    private final double minimumDetectionConfidence;

    MeasurementCheck(double minimumDetectionConfidence) {
        this.minimumDetectionConfidence = minimumDetectionConfidence;
    }

    @Override
    public Optional<String> visitBehavior(BehaviorSignal behavior) {
        if (!hasFiniteMeasurements(behavior) || behavior.detectionConfidence() < minimumDetectionConfidence) {
            return Optional.of("Behavior event has invalid measurements or low detection confidence");
        }
        return Optional.empty();
    }

    @Override
    public Optional<String> visitAudio(AudioSignal audio) {
        return Double.isFinite(audio.probability()) ? Optional.empty()
                : Optional.of("Audio event has invalid measurements");
    }

    @Override
    public Optional<String> visitWeight(WeightSignal weight) {
        return Double.isFinite(weight.grams()) ? Optional.empty()
                : Optional.of("Weight event has invalid measurements");
    }

    private static boolean hasFiniteMeasurements(BehaviorSignal behavior) {
        return Double.isFinite(behavior.stillSeconds())
                && Double.isFinite(behavior.avgGroupDistance())
                && Double.isFinite(behavior.probAnomaly())
                && Double.isFinite(behavior.detectionConfidence());
    }
}
