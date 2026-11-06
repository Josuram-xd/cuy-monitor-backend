package com.cuymonitor.backend.application;

import com.cuymonitor.backend.adapter.out.notification.DatabaseAlertObserver;
import com.cuymonitor.backend.adapter.out.persistence.memory.InMemoryAlertRepository;
import com.cuymonitor.backend.adapter.out.persistence.memory.InMemoryWeightReadingRepository;
import com.cuymonitor.backend.domain.health.weight.WeightTrendPolicy;
import com.cuymonitor.backend.domain.model.Alert;
import com.cuymonitor.backend.domain.model.EventType;
import com.cuymonitor.backend.domain.model.HealthStatus;
import com.cuymonitor.backend.domain.model.WeightReading;
import com.cuymonitor.backend.domain.notification.AlertPublisher;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class CageWeightEvaluatorTest {

    private static final Instant NOW = Instant.parse("2026-11-05T12:00:00Z");

    private final InMemoryWeightReadingRepository weights = new InMemoryWeightReadingRepository();
    private final InMemoryAlertRepository alerts = new InMemoryAlertRepository();
    private CageWeightEvaluator evaluator;

    @BeforeEach
    void setUp() {
        evaluator = new CageWeightEvaluator(weights, alerts, new AlertPublisher(List.of(new DatabaseAlertObserver(alerts))),
                new WeightTrendPolicy(0.1, Duration.ofHours(24), Duration.ofDays(6), 3));
    }

    private void readings(String cage, double before, double recent) {
        for (int i = 2; i <= 4; i++) {
            weights.save(new WeightReading(null, cage, before, true, NOW.minus(Duration.ofDays(i))));
            weights.save(new WeightReading(null, cage, recent, true, NOW.minus(Duration.ofHours(i))));
        }
    }

    @Test
    void aLighterCageGetsOneAlertWhileItIsOpen() {
        readings("cage-1", 2000, 1600);

        evaluator.evaluate("cage-1", NOW);
        evaluator.evaluate("cage-1", NOW.plusSeconds(10));

        assertThat(alerts.findAll()).singleElement().satisfies(alert -> {
            assertThat(alert.getCageCode()).isEqualTo("cage-1");
            assertThat(alert.getType()).isEqualTo(EventType.WEIGHT);
            assertThat(alert.getLevel()).isEqualTo(HealthStatus.ALERT);
            assertThat(alert.getMessage()).isEqualTo("El peso de la jaula bajó un 20 % frente a los días anteriores");
        });
    }

    @Test
    void onceReviewedTheNextDropWarnsAgain() {
        readings("cage-1", 2000, 1600);
        evaluator.evaluate("cage-1", NOW);
        Alert first = alerts.findAll().getFirst();
        first.markReviewed(NOW.plusSeconds(5));
        alerts.save(first);

        evaluator.evaluate("cage-1", NOW.plusSeconds(10));

        assertThat(alerts.findAll()).hasSize(2);
    }

    @Test
    void aSteadyCageOrAnotherCageGetsNothing() {
        readings("cage-1", 2000, 1990);
        readings("cage-2", 2000, 1500);

        evaluator.evaluate("cage-1", NOW);

        assertThat(alerts.findAll()).isEmpty();
    }
}
