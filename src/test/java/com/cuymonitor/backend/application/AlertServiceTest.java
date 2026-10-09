package com.cuymonitor.backend.application;

import com.cuymonitor.backend.adapter.out.persistence.memory.InMemoryAlertRepository;
import com.cuymonitor.backend.application.fake.MutableClock;
import com.cuymonitor.backend.domain.exception.AlertNotFoundException;
import com.cuymonitor.backend.domain.model.Alert;
import com.cuymonitor.backend.domain.model.AlertStatus;
import com.cuymonitor.backend.domain.model.EventType;
import com.cuymonitor.backend.domain.model.HealthStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AlertServiceTest {

    private static final Instant NOW = Instant.parse("2026-10-05T14:32:00Z");

    private InMemoryAlertRepository alerts;
    private MutableClock clock;
    private AlertService service;
    private Alert older;
    private Alert newer;

    @BeforeEach
    void setUp() {
        alerts = new InMemoryAlertRepository();
        clock = new MutableClock(NOW);
        service = new AlertService(alerts, clock);
        older = alerts.save(Alert.forCage("cage-1", EventType.AUDIO, HealthStatus.ALERT, "Older", NOW.minusSeconds(600)));
        newer = alerts.save(Alert.forCage("cage-1", EventType.WEIGHT, HealthStatus.CRITICAL, "Newer", NOW.minusSeconds(60)));
    }

    @Test
    void listsEveryAlertNewestFirstWithoutAFilter() {
        assertThat(service.list(null)).extracting(Alert::getMessage).containsExactly("Newer", "Older");
    }

    @Test
    void filtersByStatus() {
        service.markReviewed(older.getId());

        assertThat(service.list(AlertStatus.OPEN)).extracting(Alert::getMessage).containsExactly("Newer");
        assertThat(service.list(AlertStatus.REVIEWED)).extracting(Alert::getMessage).containsExactly("Older");
    }

    @Test
    void markingReviewedSetsTheStatusAndTheDate() {
        Alert reviewed = service.markReviewed(newer.getId());

        assertThat(reviewed.getStatus()).isEqualTo(AlertStatus.REVIEWED);
        assertThat(reviewed.getReviewedAt()).isEqualTo(NOW);
        assertThat(alerts.findById(newer.getId()).orElseThrow().getStatus()).isEqualTo(AlertStatus.REVIEWED);
    }

    @Test
    void markingAReviewedAlertAgainKeepsTheFirstDate() {
        service.markReviewed(newer.getId());
        clock.advance(Duration.ofHours(1));

        Alert again = service.markReviewed(newer.getId());

        assertThat(again.getStatus()).isEqualTo(AlertStatus.REVIEWED);
        assertThat(again.getReviewedAt()).isEqualTo(NOW);
    }

    @Test
    void anUnknownAlertIsNotFound() {
        assertThatThrownBy(() -> service.markReviewed(999)).isInstanceOf(AlertNotFoundException.class);
    }
}
