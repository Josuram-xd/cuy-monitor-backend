package com.cuymonitor.backend.domain.model;

import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

class HealthSignalVisitorTest {

    private static final BehaviorSignal BEHAVIOR = new BehaviorSignal("RED", 60, 10, 1, 0, 0.2, 0.1, 0.9);
    private static final AudioSignal AUDIO = new AudioSignal(AudioLabel.DISTRESS, 0.9, 1500);
    private static final WeightSignal WEIGHT = new WeightSignal(820.5, true);

    @Test
    void acceptCallsTheVisitorOfTheSignalType() {
        HealthSignalVisitor<String> names = new HealthSignalVisitor<>() {
            @Override
            public String visitBehavior(BehaviorSignal signal) {
                return "behavior";
            }

            @Override
            public String visitAudio(AudioSignal signal) {
                return "audio";
            }

            @Override
            public String visitWeight(WeightSignal signal) {
                return "weight";
            }
        };

        assertThat(BEHAVIOR.accept(names)).isEqualTo("behavior");
        assertThat(AUDIO.accept(names)).isEqualTo("audio");
        assertThat(WEIGHT.accept(names)).isEqualTo("weight");
    }

    @Test
    void signalDurationIsTheWindowOrTheClipOrNothing() {
        assertThat(BEHAVIOR.accept(SignalDuration.INSTANCE)).isEqualTo(Duration.ofSeconds(60));
        assertThat(AUDIO.accept(SignalDuration.INSTANCE)).isEqualTo(Duration.ofMillis(1500));
        assertThat(WEIGHT.accept(SignalDuration.INSTANCE)).isEqualTo(Duration.ZERO);
    }
}
