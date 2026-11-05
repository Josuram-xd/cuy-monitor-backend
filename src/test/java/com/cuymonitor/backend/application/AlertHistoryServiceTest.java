package com.cuymonitor.backend.application;

import com.cuymonitor.backend.adapter.out.persistence.memory.InMemoryAlertRepository;
import com.cuymonitor.backend.domain.model.Alert;
import com.cuymonitor.backend.domain.model.AlertHistoryQuery;
import com.cuymonitor.backend.domain.model.AlertStatus;
import com.cuymonitor.backend.domain.model.EventType;
import com.cuymonitor.backend.domain.model.HealthStatus;
import com.cuymonitor.backend.domain.model.Paging;
import com.cuymonitor.backend.domain.model.TimeRange;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class AlertHistoryServiceTest {

    private static final Instant NOW = Instant.parse("2026-11-04T12:00:00Z");
    private static final TimeRange WEEK = new TimeRange(NOW.minus(Duration.ofDays(7)), NOW);

    private final InMemoryAlertRepository alerts = new InMemoryAlertRepository();
    private final AlertHistoryService service = new AlertHistoryService(alerts);

    @BeforeEach
    void setUp() {
        for (int hour = 1; hour <= 25; hour++) {
            HealthStatus level = hour % 5 == 0 ? HealthStatus.CRITICAL : HealthStatus.ALERT;
            alerts.save(Alert.restore(null, "cage-1", 4L, level, EventType.BEHAVIOR, "x", AlertStatus.REVIEWED,
                    NOW.minus(Duration.ofHours(hour)), NOW));
        }
        alerts.save(Alert.forCage("cage-2", EventType.AUDIO, HealthStatus.ALERT, "x", NOW.minusSeconds(60)));
        alerts.save(Alert.forCage("cage-1", EventType.WEIGHT, HealthStatus.ALERT, "old", NOW.minus(Duration.ofDays(9))));
    }

    private static AlertHistoryQuery query(Set<String> cages, HealthStatus level, Paging paging) {
        return new AlertHistoryQuery(cages, null, level, null, null, WEEK, paging);
    }

    @Test
    void givesOnePageNewestFirstAndTheTotal() {
        var page = service.history(query(Set.of("cage-1"), null, new Paging(0, 10)));

        assertThat(page.items()).hasSize(10);
        assertThat(page.items().getFirst().getCreatedAt()).isEqualTo(NOW.minus(Duration.ofHours(1)));
        assertThat(page.totalItems()).isEqualTo(25);
        assertThat(page.totalPages()).isEqualTo(3);
        assertThat(service.history(query(Set.of("cage-1"), null, new Paging(2, 10))).items()).hasSize(5);
    }

    @Test
    void onlyTheReadableCagesTheRangeAndTheFiltersCount() {
        var critical = service.history(query(Set.of("cage-1"), HealthStatus.CRITICAL, Paging.of(null, null)));
        var both = service.history(query(Set.of("cage-1", "cage-2"), null, Paging.of(null, 100)));

        assertThat(critical.totalItems()).isEqualTo(5);
        assertThat(both.totalItems()).isEqualTo(26);
        assertThat(both.items()).noneMatch(alert -> alert.getMessage().equals("old"));
    }

    @Test
    void noReadableCageMeansNothingNotEverything() {
        assertThat(service.history(query(Set.of(), null, Paging.of(null, null))).totalItems()).isZero();
    }
}
