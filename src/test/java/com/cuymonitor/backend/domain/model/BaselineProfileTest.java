package com.cuymonitor.backend.domain.model;

import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.within;

class BaselineProfileTest {

    private static final Instant NOW = Instant.parse("2026-10-05T14:00:00Z");

    private static BehaviorSignal window(double stillSeconds, int feederVisits, double groupDistance,
                                         double probAnomaly) {
        return new BehaviorSignal("RED", 60, stillSeconds, feederVisits, 1, groupDistance, probAnomaly, 0.9);
    }

    @Test
    void theFirstWindowReplacesTheDefault() {
        var learned = BaselineProfile.defaultFor(1).learnFrom(window(10, 3, 0.2, 0.1), NOW, 60);

        assertThat(learned.avgStillSeconds()).isEqualTo(10);
        assertThat(learned.avgFeederVisits()).isEqualTo(3);
        assertThat(learned.sampleCount()).isEqualTo(1);
        assertThat(learned.updatedAt()).isEqualTo(NOW);
    }

    @Test
    void beforeTheWarmupItIsThePlainAverage() {
        var profile = BaselineProfile.defaultFor(1)
                .learnFrom(window(10, 2, 0.2, 0.1), NOW, 60)
                .learnFrom(window(20, 4, 0.4, 0.1), NOW, 60);

        assertThat(profile.avgStillSeconds()).isCloseTo(15, within(1e-9));
        assertThat(profile.avgFeederVisits()).isCloseTo(3, within(1e-9));
        assertThat(profile.avgGroupDistance()).isCloseTo(0.3, within(1e-9));
    }

    @Test
    void afterTheWarmupOneWindowMovesItOnlyALittle() {
        var profile = new BaselineProfile(1, 10, 2, 0.2, NOW, 100);

        var moved = profile.learnFrom(window(40, 2, 0.2, 0.1), NOW, 10);

        // weight 1/10: a single odd window can't rewrite what is normal
        assertThat(moved.avgStillSeconds()).isCloseTo(13, within(1e-9));
        assertThat(moved.sampleCount()).isEqualTo(101);
    }

    @Test
    void aProfileIsLearnedOnlyWithEnoughWindows() {
        assertThat(new BaselineProfile(1, 10, 2, 0.2, NOW, 29).isLearned(30)).isFalse();
        assertThat(new BaselineProfile(1, 10, 2, 0.2, NOW, 30).isLearned(30)).isTrue();
    }

    @Test
    void thresholdsDecideWhatIsAnomalous() {
        var profile = new BaselineProfile(1, 20, 2, 0.2, NOW, 50);
        var stillWindow = window(35, 0, 0.2, 0.1);

        assertThat(profile.isAnomalous(stillWindow, new AnomalyThresholds(0.7, 1.5, 2.0))).isTrue();
        assertThat(profile.isAnomalous(stillWindow, new AnomalyThresholds(0.7, 2.0, 2.0))).isFalse();
        assertThat(profile.isAnomalous(window(10, 2, 0.2, 0.6), new AnomalyThresholds(0.5, 1.5, 2.0))).isTrue();
        assertThat(profile.anomalyReason(window(10, 2, 0.5, 0.1))).isEqualTo("se alejó del grupo");
    }

    @Test
    void invalidThresholdsAreRejected() {
        assertThatThrownBy(() -> new AnomalyThresholds(0, 1.5, 2.0)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new AnomalyThresholds(0.7, 1.0, 2.0)).isInstanceOf(IllegalArgumentException.class);
    }
}
