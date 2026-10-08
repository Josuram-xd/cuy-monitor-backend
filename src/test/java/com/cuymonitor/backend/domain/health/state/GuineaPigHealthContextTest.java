package com.cuymonitor.backend.domain.health.state;

import com.cuymonitor.backend.domain.health.fake.HealthFakes;
import com.cuymonitor.backend.domain.model.Alert;
import com.cuymonitor.backend.domain.model.EventType;
import com.cuymonitor.backend.domain.model.GuineaPig;
import com.cuymonitor.backend.domain.model.HealthStatus;
import com.cuymonitor.backend.domain.model.MarkColor;
import com.cuymonitor.backend.domain.model.StateTransition;
import com.cuymonitor.backend.domain.notification.AlertPublisher;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;

class GuineaPigHealthContextTest {

    private static final Instant CREATED = Instant.parse("2026-10-05T10:00:00Z");
    private static final Instant NOW = Instant.parse("2026-10-05T14:00:00Z");

    private final HealthFakes.Transitions transitions = new HealthFakes.Transitions();
    private final HealthFakes.RecordingObserver observer = new HealthFakes.RecordingObserver();
    private final AlertPublisher publisher = new AlertPublisher();

    @BeforeEach
    void setUp() {
        publisher.subscribe(observer);
    }

    @Test
    void sustainedAnomalyMovesNormalToObservedAndStoresTheTransition() {
        GuineaPig pig = pig(HealthStatus.NORMAL);

        StateTransition transition = context(pig).onSustainedAnomaly("estuvo quieto").orElseThrow();

        assertThat(transition.fromStatus()).isEqualTo(HealthStatus.NORMAL);
        assertThat(transition.toStatus()).isEqualTo(HealthStatus.OBSERVED);
        assertThat(transition.reason()).isEqualTo("estuvo quieto");
        assertThat(transition.occurredAt()).isEqualTo(NOW);
        assertThat(pig.getStatus()).isEqualTo(HealthStatus.OBSERVED);
        assertThat(pig.getStatusSince()).isEqualTo(NOW);
        assertThat(transitions.saved).containsExactly(transition);
    }

    @Test
    void goingToObservedDoesNotWarnTheFarmer() {
        context(pig(HealthStatus.NORMAL)).onSustainedAnomaly("estuvo quieto");

        assertThat(observer.received).isEmpty();
    }

    @Test
    void goingToAlertPublishesAnAlert() {
        context(pig(HealthStatus.OBSERVED)).onSustainedAnomaly("estuvo quieto");

        Alert alert = observer.received.getFirst();
        assertThat(alert.getLevel()).isEqualTo(HealthStatus.ALERT);
        assertThat(alert.getType()).isEqualTo(EventType.BEHAVIOR);
        assertThat(alert.getGuineaPigId()).isEqualTo(1L);
        assertThat(alert.getMessage()).isEqualTo("Canela está en alerta: estuvo quieto");
    }

    @Test
    void goingToCriticalPublishesACriticalAlert() {
        context(pig(HealthStatus.ALERT)).onSustainedAnomaly("estuvo quieto");

        assertThat(observer.received.getFirst().getLevel()).isEqualTo(HealthStatus.CRITICAL);
        assertThat(observer.received.getFirst().getMessage()).contains("estado crítico");
    }

    @Test
    void stayingCriticalStoresNothing() {
        GuineaPig pig = pig(HealthStatus.CRITICAL);

        assertThat(context(pig).onSustainedAnomaly("estuvo quieto")).isEmpty();
        assertThat(transitions.saved).isEmpty();
        assertThat(observer.received).isEmpty();
        assertThat(pig.getStatusSince()).isEqualTo(CREATED);
    }

    @Test
    void recoveryGoesBackToNormalWithoutAnAlert() {
        GuineaPig pig = pig(HealthStatus.CRITICAL);

        StateTransition transition = context(pig).onSustainedRecovery("volvió a lo normal").orElseThrow();

        assertThat(transition.toStatus()).isEqualTo(HealthStatus.NORMAL);
        assertThat(pig.getStatus()).isEqualTo(HealthStatus.NORMAL);
        assertThat(observer.received).isEmpty();
    }

    @Test
    void recoveryOfANormalGuineaPigChangesNothing() {
        assertThat(context(pig(HealthStatus.NORMAL)).onSustainedRecovery("volvió a lo normal")).isEmpty();
    }

    private GuineaPigHealthContext context(GuineaPig pig) {
        return new GuineaPigHealthContext(pig, transitions, publisher, Clock.fixed(NOW, ZoneOffset.UTC));
    }

    private static GuineaPig pig(HealthStatus status) {
        return GuineaPig.restore(1L, "cage-1", "Canela", MarkColor.RED, status, CREATED, true, CREATED);
    }
}
