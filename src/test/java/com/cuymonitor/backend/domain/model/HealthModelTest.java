package com.cuymonitor.backend.domain.model;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class HealthModelTest {

    private static final Instant NOW = Instant.parse("2026-10-05T14:00:00Z");

    @Test
    void deactivatingKeepsTheRestOfTheGuineaPig() {
        GuineaPig pig = GuineaPig.register("cage-1", "Canela", MarkColor.RED, NOW);

        pig.deactivate();

        assertThat(pig.isActive()).isFalse();
        assertThat(pig.getName()).isEqualTo("Canela");
        assertThat(pig.getStatus()).isEqualTo(HealthStatus.NORMAL);
    }

    @Test
    void healthStatusOrderGoesFromNormalToCritical() {
        assertThat(HealthStatus.CRITICAL.isWorseThan(HealthStatus.ALERT)).isTrue();
        assertThat(HealthStatus.NORMAL.isWorseThan(HealthStatus.OBSERVED)).isFalse();
        assertThat(HealthStatus.worst(HealthStatus.OBSERVED, HealthStatus.ALERT)).isEqualTo(HealthStatus.ALERT);
        assertThat(HealthStatus.ALERT.raisesAlert()).isTrue();
        assertThat(HealthStatus.OBSERVED.raisesAlert()).isFalse();
    }

    @Test
    void registeredGuineaPigStartsNormal() {
        GuineaPig pig = GuineaPig.register("cage-1", "  Canela ", MarkColor.RED, NOW);

        assertThat(pig.getId()).isNull();
        assertThat(pig.getName()).isEqualTo("Canela");
        assertThat(pig.getStatus()).isEqualTo(HealthStatus.NORMAL);
        assertThat(pig.getStatusSince()).isEqualTo(NOW);
        assertThat(pig.isActive()).isTrue();
    }

    @Test
    void guineaPigNameIsRequiredAndShort() {
        assertThatThrownBy(() -> GuineaPig.register("cage-1", " ", MarkColor.RED, NOW))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> GuineaPig.register("cage-1", "a".repeat(101), MarkColor.RED, NOW))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void changeStatusOnlyMovesStatusSinceWhenTheStatusChanges() {
        GuineaPig pig = GuineaPig.register("cage-1", "Canela", MarkColor.RED, NOW);
        Instant later = NOW.plusSeconds(60);

        pig.changeStatus(HealthStatus.NORMAL, later);
        assertThat(pig.getStatusSince()).isEqualTo(NOW);

        pig.changeStatus(HealthStatus.OBSERVED, later);
        assertThat(pig.getStatus()).isEqualTo(HealthStatus.OBSERVED);
        assertThat(pig.getStatusSince()).isEqualTo(later);
    }

    @Test
    void alertStartsOpenAndCanBeReviewedOnce() {
        Alert alert = Alert.forCage("cage-1", EventType.AUDIO, HealthStatus.ALERT, "chillidos", NOW);
        Instant later = NOW.plusSeconds(60);

        assertThat(alert.getStatus()).isEqualTo(AlertStatus.OPEN);
        assertThat(alert.getGuineaPigId()).isNull();

        alert.markReviewed(later);
        alert.markReviewed(later.plusSeconds(60));

        assertThat(alert.getStatus()).isEqualTo(AlertStatus.REVIEWED);
        assertThat(alert.getReviewedAt()).isEqualTo(later);
    }

    @Test
    void alertLevelMustBeAlertOrCritical() {
        assertThatThrownBy(() -> Alert.forCage("cage-1", EventType.AUDIO, HealthStatus.OBSERVED, "x", NOW))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void alertIdCanOnlyBeAssignedOnce() {
        Alert alert = Alert.forCage("cage-1", EventType.AUDIO, HealthStatus.ALERT, "x", NOW);
        alert.assignId(7);

        assertThat(alert.getId()).isEqualTo(7);
        assertThatThrownBy(() -> alert.assignId(8)).isInstanceOf(IllegalStateException.class);
    }

    @Test
    void baselineFlagsWindowsTheClassifierMarks() {
        BaselineProfile baseline = BaselineProfile.defaultFor(1);

        assertThat(baseline.isAnomalous(window(10, 2, 0.2, 0.75))).isTrue();
        assertThat(baseline.isAnomalous(window(10, 2, 0.2, 0.1))).isFalse();
    }

    @Test
    void baselineFlagsLongStillnessWithoutEating() {
        BaselineProfile baseline = BaselineProfile.defaultFor(1);

        assertThat(baseline.isAnomalous(window(50, 0, 0.2, 0.1))).isTrue();
        assertThat(baseline.isAnomalous(window(50, 1, 0.2, 0.1))).isFalse();
    }

    @Test
    void baselineFlagsIsolationFromTheGroup() {
        assertThat(BaselineProfile.defaultFor(1).anomalyReason(window(10, 2, 0.9, 0.1))).contains("grupo");
    }

    @Test
    void behaviorSignalRejectsValuesOutsideTheContract() {
        assertThatThrownBy(() -> new BehaviorSignal(MarkColor.RED, 60, 61, 0, 0, 0.1, 0.1, 0.9))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new BehaviorSignal(MarkColor.RED, 60, 10, -1, 0, 0.1, 0.1, 0.9))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new BehaviorSignal(MarkColor.RED, 60, 10, 0, 0, 1.2, 0.1, 0.9))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void healthEventTypeComesFromItsSignal() {
        HealthEvent event = new HealthEvent(UUID.randomUUID(), "cage-1", NOW, "arduino", new WeightSignal(812.4, true));

        assertThat(event.type()).isEqualTo(EventType.WEIGHT);
    }

    private static BehaviorSignal window(double stillSeconds, int feederVisits, double groupDistance, double probAnomaly) {
        return new BehaviorSignal(MarkColor.RED, 60, stillSeconds, feederVisits, 1, groupDistance, probAnomaly, 0.9);
    }
}
