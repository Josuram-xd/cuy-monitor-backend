package com.cuymonitor.backend.application;

import com.cuymonitor.backend.adapter.out.notification.DatabaseAlertObserver;
import com.cuymonitor.backend.adapter.out.persistence.memory.InMemoryAlertRepository;
import com.cuymonitor.backend.adapter.out.persistence.memory.InMemoryBaselineProfileRepository;
import com.cuymonitor.backend.adapter.out.persistence.memory.InMemoryCageRepository;
import com.cuymonitor.backend.adapter.out.persistence.memory.InMemoryEventRepository;
import com.cuymonitor.backend.adapter.out.persistence.memory.InMemoryGuineaPigRepository;
import com.cuymonitor.backend.adapter.out.persistence.memory.InMemoryStateTransitionRepository;
import com.cuymonitor.backend.adapter.out.persistence.memory.InMemoryWeightReadingRepository;
import com.cuymonitor.backend.domain.health.weight.WeightTrendPolicy;
import com.cuymonitor.backend.application.fake.MutableClock;
import com.cuymonitor.backend.application.fake.Photos;
import com.cuymonitor.backend.domain.health.chain.BehaviorThresholdHandler;
import com.cuymonitor.backend.domain.health.chain.HandlerChainBuilder;
import com.cuymonitor.backend.domain.health.chain.IdentificationHandler;
import com.cuymonitor.backend.domain.health.chain.SustainedAnomalyHandler;
import com.cuymonitor.backend.domain.health.chain.SustainedRecoveryHandler;
import com.cuymonitor.backend.domain.health.chain.ValidationHandler;
import com.cuymonitor.backend.domain.health.state.EscalationPolicy;
import com.cuymonitor.backend.domain.model.AnomalyThresholds;
import com.cuymonitor.backend.domain.model.BehaviorSignal;
import com.cuymonitor.backend.domain.model.GuineaPig;
import com.cuymonitor.backend.domain.model.HealthEvent;
import com.cuymonitor.backend.domain.model.HealthStatus;
import com.cuymonitor.backend.domain.notification.AlertPublisher;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Task 10: the real rules, with time. One anomalous window per minute; an anomaly is confirmed after 3 in a row,
 * OBSERVED becomes ALERT after 30 minutes, ALERT becomes CRITICAL after 60, and 5 normal windows recover.
 */
class HealthRulesTest {

    private static final Instant START = Instant.parse("2026-11-04T08:00:00Z");

    private MutableClock clock;
    private InMemoryGuineaPigRepository guineaPigs;
    private InMemoryAlertRepository alerts;
    private EventProcessingService service;
    private long canela;

    @BeforeEach
    void setUp() {
        clock = new MutableClock(START);
        guineaPigs = new InMemoryGuineaPigRepository();
        alerts = new InMemoryAlertRepository();
        var events = new InMemoryEventRepository();
        var chain = new HandlerChainBuilder(
                new ValidationHandler(clock, Duration.ofMinutes(10), 0.5),
                new IdentificationHandler(guineaPigs),
                new BehaviorThresholdHandler(new InMemoryBaselineProfileRepository()),
                new SustainedAnomalyHandler(events, 3),
                new SustainedRecoveryHandler(events, 5)).build();
        var weights = new InMemoryWeightReadingRepository();
        var recorder = new EventIngestionService(new InMemoryCageRepository(), guineaPigs, events, weights);
        var publisher = new AlertPublisher(List.of(new DatabaseAlertObserver(alerts)));
        service = new EventProcessingService(recorder, chain, guineaPigs, new InMemoryStateTransitionRepository(),
                publisher, clock, 0.8,
                new EscalationPolicy(Duration.ofMinutes(30), Duration.ofMinutes(60)),
                // learning is covered in BaselineLearnerTest; a separate store keeps these rules fixed
                new BaselineLearner(new InMemoryBaselineProfileRepository(), AnomalyThresholds.defaults(), 60),
                new CageWeightEvaluator(weights, alerts, publisher,
                        new WeightTrendPolicy(0.1, Duration.ofHours(24), Duration.ofDays(6), 5)));
        canela = guineaPigs.save(GuineaPig.register("cage-1", "RED", Photos.png(), START.minus(Duration.ofDays(3))))
                .getId();
    }

    @Test
    void aSustainedAnomalyIsWatchedFirstAndOnlyReportedAfter30Minutes() {
        minutes(3, 0.9);
        assertThat(status()).isEqualTo(HealthStatus.OBSERVED);

        minutes(28, 0.9);
        assertThat(status()).as("29 minutes in OBSERVED").isEqualTo(HealthStatus.OBSERVED);
        assertThat(alerts.findAll()).isEmpty();

        minutes(2, 0.9);
        assertThat(status()).isEqualTo(HealthStatus.ALERT);
        assertThat(alerts.findAll()).hasSize(1);
    }

    @Test
    void criticalNeedsAnotherHourOfAnomalyAfterAlert() {
        minutes(33, 0.9);
        assertThat(status()).isEqualTo(HealthStatus.ALERT);

        minutes(59, 0.9);
        assertThat(status()).isEqualTo(HealthStatus.ALERT);

        minutes(2, 0.9);
        assertThat(status()).isEqualTo(HealthStatus.CRITICAL);
        assertThat(alerts.findAll()).extracting(a -> a.getLevel())
                .containsExactly(HealthStatus.CRITICAL, HealthStatus.ALERT);
    }

    @Test
    void recoveringTakesFiveNormalWindowsInARow() {
        minutes(33, 0.9);

        minutes(4, 0.1);
        assertThat(status()).isEqualTo(HealthStatus.ALERT);

        minutes(1, 0.1);
        assertThat(status()).isEqualTo(HealthStatus.NORMAL);
    }

    @Test
    void anAnomalousWindowInTheMiddleRestartsTheRecovery() {
        minutes(33, 0.9);

        minutes(4, 0.1);
        minutes(1, 0.9);
        minutes(4, 0.1);

        assertThat(status()).isEqualTo(HealthStatus.ALERT);
    }

    @Test
    void theTimeAtALevelStartsAgainAfterARecovery() {
        minutes(33, 0.9);
        minutes(5, 0.1);
        assertThat(status()).isEqualTo(HealthStatus.NORMAL);

        minutes(3, 0.9);
        minutes(10, 0.9);

        assertThat(status()).as("only 13 minutes of the new anomaly").isEqualTo(HealthStatus.OBSERVED);
    }

    // one window per minute for n minutes, the clock moving with them
    private void minutes(int n, double probAnomaly) {
        for (int i = 0; i < n; i++) {
            clock.advance(Duration.ofMinutes(1));
            service.process(new HealthEvent(UUID.randomUUID(), "cage-1", clock.instant(), "test",
                    new BehaviorSignal("RED", 60, 20, 2, 1, 0.2, probAnomaly, 0.95)));
        }
    }

    private HealthStatus status() {
        return guineaPigs.findById(canela).orElseThrow().getStatus();
    }
}
