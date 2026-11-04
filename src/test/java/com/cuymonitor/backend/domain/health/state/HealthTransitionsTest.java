package com.cuymonitor.backend.domain.health.state;

import com.cuymonitor.backend.domain.model.HealthStatus;
import com.cuymonitor.backend.domain.model.StateTransition;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.time.Duration;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Every arrow of the state diagram in ARCHITECTURE.md 3.7, with plain JUnit and no Spring. */
class HealthTransitionsTest {

    private static final Instant NOW = Instant.parse("2026-11-04T10:00:00Z");

    @Test
    void eachConfirmedAnomalyGoesUpOneLevel() {
        var context = new GuineaPigHealthContext(1L, new NormalState());

        assertThat(context.anomalyConfirmed("still", NOW)).map(StateTransition::toStatus)
                .contains(HealthStatus.OBSERVED);
        assertThat(context.anomalyConfirmed("still", NOW)).map(StateTransition::toStatus)
                .contains(HealthStatus.ALERT);
        assertThat(context.anomalyConfirmed("still", NOW)).map(StateTransition::toStatus)
                .contains(HealthStatus.CRITICAL);
    }

    @Test
    void criticalIsTheTopAndRecordsNothingMore() {
        var context = new GuineaPigHealthContext(1L, new CriticalState());

        assertThat(context.anomalyConfirmed("still", NOW)).isEmpty();
        assertThat(context.status()).isEqualTo(HealthStatus.CRITICAL);
    }

    @ParameterizedTest
    @EnumSource(value = HealthStatus.class, names = {"OBSERVED", "ALERT", "CRITICAL"})
    void anyLevelRecoversStraightToNormal(HealthStatus from) {
        var context = new GuineaPigHealthContext(1L, HealthStates.of(from));

        var transition = context.recovered(NOW).orElseThrow();

        assertThat(transition.fromStatus()).isEqualTo(from);
        assertThat(transition.toStatus()).isEqualTo(HealthStatus.NORMAL);
        assertThat(context.status()).isEqualTo(HealthStatus.NORMAL);
    }

    @Test
    void recoveringWhileNormalRecordsNothing() {
        assertThat(new GuineaPigHealthContext(1L, new NormalState()).recovered(NOW)).isEmpty();
    }

    @Test
    void theTransitionKeepsWhoMovedWhyAndWhen() {
        var transition = new GuineaPigHealthContext(9L, new NormalState())
                .anomalyConfirmed("Anomaly in 3 consecutive windows", NOW).orElseThrow();

        assertThat(transition.guineaPigId()).isEqualTo(9L);
        assertThat(transition.fromStatus()).isEqualTo(HealthStatus.NORMAL);
        assertThat(transition.reason()).isEqualTo("Anomaly in 3 consecutive windows");
        assertThat(transition.occurredAt()).isEqualTo(NOW);
    }

    @Test
    void onlyAlertAndCriticalWarnTheBreeder() {
        assertThat(new NormalState().raisesAlert()).isFalse();
        assertThat(new ObservedState().raisesAlert()).isFalse();
        assertThat(new AlertState().raisesAlert()).isTrue();
        assertThat(new CriticalState().raisesAlert()).isTrue();
    }

    @ParameterizedTest
    @EnumSource(HealthStatus.class)
    void aStoredStatusRebuildsTheSameState(HealthStatus status) {
        assertThat(HealthStates.of(status).status()).isEqualTo(status);
    }

    @Test
    void observedOnlyBecomesAlertOnceTheAnomalyHasLastedTheEscalationTime() {
        var policy = new EscalationPolicy(Duration.ofMinutes(30), Duration.ofMinutes(60));

        assertThat(new ObservedState().onAnomaly(Duration.ofMinutes(29), policy).status())
                .isEqualTo(HealthStatus.OBSERVED);
        assertThat(new ObservedState().onAnomaly(Duration.ofMinutes(30), policy).status())
                .isEqualTo(HealthStatus.ALERT);
        assertThat(new AlertState().onAnomaly(Duration.ofMinutes(59), policy).status())
                .isEqualTo(HealthStatus.ALERT);
        assertThat(new AlertState().onAnomaly(Duration.ofMinutes(60), policy).status())
                .isEqualTo(HealthStatus.CRITICAL);
        assertThat(new NormalState().onAnomaly(Duration.ZERO, policy).status())
                .as("the first confirmed anomaly is enough to start watching").isEqualTo(HealthStatus.OBSERVED);
    }

    @Test
    void theContextCountsTheTimeFromWhenItReachedItsLevel() {
        var policy = new EscalationPolicy(Duration.ofMinutes(30), Duration.ofMinutes(60));
        var context = new GuineaPigHealthContext(1L, new ObservedState(), NOW.minus(Duration.ofMinutes(10)));

        assertThat(context.anomalyConfirmed("still", NOW, policy)).isEmpty();
        assertThat(context.anomalyConfirmed("still", NOW.plus(Duration.ofMinutes(20)), policy))
                .map(StateTransition::toStatus).contains(HealthStatus.ALERT);
    }

    @Test
    void negativeEscalationTimesAreRejected() {
        assertThatThrownBy(
                        () -> new EscalationPolicy(Duration.ofMinutes(-1), Duration.ZERO))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
