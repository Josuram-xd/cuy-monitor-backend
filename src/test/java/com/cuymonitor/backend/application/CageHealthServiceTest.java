package com.cuymonitor.backend.application;

import com.cuymonitor.backend.adapter.out.persistence.memory.InMemoryAlertRepository;
import com.cuymonitor.backend.adapter.out.persistence.memory.InMemoryCageRepository;
import com.cuymonitor.backend.adapter.out.persistence.memory.InMemoryEventRepository;
import com.cuymonitor.backend.adapter.out.persistence.memory.InMemoryGuineaPigRepository;
import com.cuymonitor.backend.adapter.out.persistence.memory.InMemoryWeightReadingRepository;
import com.cuymonitor.backend.application.fake.MutableClock;
import com.cuymonitor.backend.domain.exception.CageNotFoundException;
import com.cuymonitor.backend.domain.model.Alert;
import com.cuymonitor.backend.domain.model.AudioLabel;
import com.cuymonitor.backend.domain.model.AudioSignal;
import com.cuymonitor.backend.domain.model.CageHealthSummary;
import com.cuymonitor.backend.domain.model.EventType;
import com.cuymonitor.backend.domain.model.GuineaPig;
import com.cuymonitor.backend.domain.model.HealthEvent;
import com.cuymonitor.backend.domain.model.HealthStatus;
import com.cuymonitor.backend.domain.model.MarkColor;
import com.cuymonitor.backend.domain.model.WeightReading;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CageHealthServiceTest {

    private static final Instant NOW = Instant.parse("2026-10-05T14:32:00Z");
    private static final String CAGE = "cage-1";

    private InMemoryGuineaPigRepository guineaPigs;
    private InMemoryAlertRepository alerts;
    private InMemoryEventRepository events;
    private InMemoryWeightReadingRepository weights;
    private CageHealthService service;

    @BeforeEach
    void setUp() {
        guineaPigs = new InMemoryGuineaPigRepository();
        alerts = new InMemoryAlertRepository();
        events = new InMemoryEventRepository();
        weights = new InMemoryWeightReadingRepository();
        service = new CageHealthService(new InMemoryCageRepository(), guineaPigs, alerts, events, weights,
                new MutableClock(NOW));
    }

    @Test
    void aCageWithNoDataIsNormalAndHasNoLastValues() {
        CageHealthSummary summary = service.getHealth(CAGE);

        assertThat(summary.status()).isEqualTo(HealthStatus.NORMAL);
        assertThat(summary.guineaPigs()).isEmpty();
        assertThat(summary.audio().lastEventAt()).isNull();
        assertThat(summary.weight().lastGrams()).isNull();
        assertThat(summary.updatedAt()).isEqualTo(NOW);
    }

    @Test
    void theCageIsAsBadAsItsWorstGuineaPig() {
        guineaPig("Canela", MarkColor.RED, HealthStatus.OBSERVED);
        guineaPig("Pelusa", MarkColor.BLUE, HealthStatus.CRITICAL);
        guineaPig("Copito", MarkColor.WHITE, HealthStatus.NORMAL);

        CageHealthSummary summary = service.getHealth(CAGE);

        assertThat(summary.status()).isEqualTo(HealthStatus.CRITICAL);
        assertThat(summary.guineaPigs()).hasSize(3);
    }

    @Test
    void anOpenAudioAlertRaisesTheAudioPartAndTheCage() {
        guineaPig("Canela", MarkColor.RED, HealthStatus.NORMAL);
        alerts.save(Alert.forCage(CAGE, EventType.AUDIO, HealthStatus.ALERT, "Distress sounds", NOW));
        events.save(new HealthEvent(UUID.randomUUID(), CAGE, NOW.minusSeconds(30), "ai-service",
                new AudioSignal(AudioLabel.DISTRESS, 0.9, 1500)), null);

        CageHealthSummary summary = service.getHealth(CAGE);

        assertThat(summary.audio().status()).isEqualTo(HealthStatus.ALERT);
        assertThat(summary.audio().lastEventAt()).isEqualTo(NOW.minusSeconds(30));
        assertThat(summary.weight().status()).isEqualTo(HealthStatus.NORMAL);
        assertThat(summary.status()).isEqualTo(HealthStatus.ALERT);
    }

    @Test
    void aReviewedAlertNoLongerCounts() {
        Alert alert = alerts.save(Alert.forCage(CAGE, EventType.WEIGHT, HealthStatus.CRITICAL, "Weight dropped", NOW));
        alert.markReviewed(NOW.plusSeconds(60));
        alerts.save(alert);

        assertThat(service.getHealth(CAGE).status()).isEqualTo(HealthStatus.NORMAL);
    }

    @Test
    void alertsOfAnotherCageAreIgnored() {
        alerts.save(Alert.forCage("cage-2", EventType.AUDIO, HealthStatus.CRITICAL, "Elsewhere", NOW));

        assertThat(service.getHealth(CAGE).status()).isEqualTo(HealthStatus.NORMAL);
    }

    @Test
    void reportsTheLastWeightReading() {
        weights.save(new WeightReading(null, CAGE, 812.4, true, NOW.minusSeconds(120)));

        CageHealthSummary summary = service.getHealth(CAGE);

        assertThat(summary.weight().lastGrams()).isEqualTo(812.4);
        assertThat(summary.weight().lastMeasuredAt()).isEqualTo(NOW.minusSeconds(120));
    }

    @Test
    void anUnknownCageIsNotFound() {
        assertThatThrownBy(() -> service.getHealth("nope")).isInstanceOf(CageNotFoundException.class);
    }

    private void guineaPig(String name, MarkColor color, HealthStatus status) {
        GuineaPig pig = guineaPigs.save(GuineaPig.register(CAGE, name, color, NOW.minusSeconds(3600)));
        pig.changeStatus(status, NOW.minusSeconds(600));
        guineaPigs.save(pig);
    }
}
