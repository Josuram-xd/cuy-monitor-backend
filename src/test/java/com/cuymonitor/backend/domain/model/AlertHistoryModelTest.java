package com.cuymonitor.backend.domain.model;

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AlertHistoryModelTest {

    private static final Instant NOW = Instant.parse("2026-11-04T12:00:00Z");
    private static final TimeRange WEEK = new TimeRange(NOW.minus(Duration.ofDays(7)), NOW);

    private static Alert alert(String cage, Long pig, HealthStatus level, EventType type, Instant at) {
        return Alert.restore(1L, cage, pig, level, type, "x", AlertStatus.OPEN, at, null);
    }

    @Test
    void pagingHasDefaultsAndLimits() {
        assertThat(Paging.of(null, null)).isEqualTo(new Paging(0, Paging.DEFAULT_SIZE));
        assertThat(new Paging(3, 20).offset()).isEqualTo(60);
        assertThatThrownBy(() -> new Paging(-1, 20)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new Paging(0, 0)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new Paging(0, Paging.MAX_SIZE + 1)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void aFilterOnlyAcceptsTheLevelsThatRaiseAnAlert() {
        assertThatThrownBy(() -> new AlertHistoryFilter(null, null, HealthStatus.NORMAL, null, null, WEEK,
                Paging.of(null, null))).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void theFilterBuildsTheQueryLimitedToTheReadableCages() {
        var filter = new AlertHistoryFilter(null, 4L, HealthStatus.ALERT, EventType.BEHAVIOR, null, WEEK,
                new Paging(1, 10));

        var query = filter.within(Set.of("cage-1"));

        assertThat(query).isEqualTo(new AlertHistoryQuery(Set.of("cage-1"), 4L, HealthStatus.ALERT,
                EventType.BEHAVIOR, null, WEEK, new Paging(1, 10)));
        assertThat(filter.within(Set.of()).readsNothing()).isTrue();
    }

    @Test
    void theQueryTellsWhichAlertsMatch() {
        var query = new AlertHistoryQuery(Set.of("cage-1"), null, HealthStatus.CRITICAL, null, null, WEEK,
                Paging.of(null, null));

        assertThat(query.matches(alert("cage-1", 4L, HealthStatus.CRITICAL, EventType.BEHAVIOR, NOW))).isTrue();
        assertThat(query.matches(alert("cage-2", 4L, HealthStatus.CRITICAL, EventType.BEHAVIOR, NOW))).isFalse();
        assertThat(query.matches(alert("cage-1", 4L, HealthStatus.ALERT, EventType.BEHAVIOR, NOW))).isFalse();
        assertThat(query.matches(alert("cage-1", null, HealthStatus.CRITICAL, EventType.AUDIO,
                NOW.minus(Duration.ofDays(8))))).isFalse();
    }

    @Test
    void aPageKnowsHowManyPagesThereAre() {
        var first = new AlertHistoryPage(List.of(), new Paging(0, 20), 41, WEEK);
        var last = new AlertHistoryPage(List.of(), new Paging(2, 20), 41, WEEK);

        assertThat(first.totalPages()).isEqualTo(3);
        assertThat(first.hasNext()).isTrue();
        assertThat(last.hasNext()).isFalse();
        assertThat(AlertHistoryPage.empty(new Paging(0, 20), WEEK).totalPages()).isZero();
    }
}
