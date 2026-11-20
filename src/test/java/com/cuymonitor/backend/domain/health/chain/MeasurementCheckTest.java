package com.cuymonitor.backend.domain.health.chain;

import com.cuymonitor.backend.domain.model.AudioLabel;
import com.cuymonitor.backend.domain.model.AudioSignal;
import com.cuymonitor.backend.domain.model.BehaviorSignal;
import com.cuymonitor.backend.domain.model.WeightSignal;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class MeasurementCheckTest {

    private final MeasurementCheck check = new MeasurementCheck(0.5);

    @Test
    void behaviorWithUsableMeasurementsIsAccepted() {
        assertThat(new BehaviorSignal("RED", 60, 10, 1, 0, 0.2, 0.1, 0.9).accept(check)).isEmpty();
    }

    @Test
    void behaviorWithLowConfidenceOrNonFiniteValueIsRejected() {
        var rejected = Optional.of("Behavior event has invalid measurements or low detection confidence");

        assertThat(new BehaviorSignal("RED", 60, 10, 1, 0, 0.2, 0.1, 0.2).accept(check)).isEqualTo(rejected);
        assertThat(new BehaviorSignal("RED", 60, 10, 1, 0, Double.NaN, 0.1, 0.9).accept(check)).isEqualTo(rejected);
    }

    @Test
    void audioWithUsableProbabilityIsAcceptedAndNonFiniteIsRejected() {
        assertThat(new AudioSignal(AudioLabel.DISTRESS, 0.9, 1500).accept(check)).isEmpty();
        assertThat(new AudioSignal(AudioLabel.DISTRESS, Double.NaN, 1500).accept(check))
                .isEqualTo(Optional.of("Audio event has invalid measurements"));
    }

    @Test
    void weightWithUsableGramsIsAcceptedAndNonFiniteIsRejected() {
        assertThat(new WeightSignal(820.5, true).accept(check)).isEmpty();
        assertThat(new WeightSignal(Double.POSITIVE_INFINITY, true).accept(check))
                .isEqualTo(Optional.of("Weight event has invalid measurements"));
    }
}
