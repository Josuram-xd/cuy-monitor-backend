package com.cuymonitor.backend.adapter.out.persistence;

import com.cuymonitor.backend.adapter.out.persistence.mapper.AlertPersistenceMapper;
import com.cuymonitor.backend.domain.model.Alert;
import com.cuymonitor.backend.domain.model.AlertHistoryQuery;
import com.cuymonitor.backend.domain.model.AlertStatus;
import com.cuymonitor.backend.domain.model.EventType;
import com.cuymonitor.backend.domain.model.HealthStatus;
import com.cuymonitor.backend.domain.model.Paging;
import com.cuymonitor.backend.domain.model.TimeRange;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

// Against the real Postgres: the JPA Specification filters and pages in the database. Each test rolls back.
@DataJpaTest(properties = "app.auth.jwt.secret=test-secret-with-at-least-32-bytes!!")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({AlertPersistenceAdapter.class, AlertPersistenceMapper.class})
class AlertHistoryPersistenceTest {

    private static final Instant NOW = Instant.now().truncatedTo(ChronoUnit.SECONDS);
    private static final TimeRange WEEK = new TimeRange(NOW.minus(Duration.ofDays(7)), NOW);

    @Autowired
    private AlertPersistenceAdapter alerts;
    @Autowired
    private JdbcTemplate jdbc;

    private String cage;
    private String otherCage;
    private long pig;

    @BeforeEach
    void createAlerts() {
        cage = newCage();
        otherCage = newCage();
        long cageId = jdbc.queryForObject("select id from cage where code = ?", Long.class, cage);
        pig = jdbc.queryForObject("insert into guinea_pig (cage_id, mark_color) values (?, 'RED') returning id",
                Long.class, cageId);
        for (int hour = 1; hour <= 12; hour++) {
            HealthStatus level = hour % 4 == 0 ? HealthStatus.CRITICAL : HealthStatus.ALERT;
            insert(cageId, pig, level, "BEHAVIOR", NOW.minus(Duration.ofHours(hour)));
        }
        insert(cageId, null, HealthStatus.ALERT, "AUDIO", NOW.minus(Duration.ofMinutes(30)));
        insert(cageId, null, HealthStatus.ALERT, "WEIGHT", NOW.minus(Duration.ofDays(10)));
        long otherId = jdbc.queryForObject("select id from cage where code = ?", Long.class, otherCage);
        insert(otherId, null, HealthStatus.CRITICAL, "AUDIO", NOW.minus(Duration.ofMinutes(5)));
    }

    private String newCage() {
        String code = "it-" + UUID.randomUUID().toString().substring(0, 8);
        jdbc.update("insert into cage (name, code) values ('IT', ?)", code);
        return code;
    }

    private void insert(long cageId, Long guineaPigId, HealthStatus level, String type, Instant at) {
        jdbc.update("insert into alert (cage_id, guinea_pig_id, level, type, message, status, created_at, reviewed_at)"
                        + " values (?, ?, ?, ?, 'x', 'REVIEWED', ?, ?)",
                cageId, guineaPigId, level.name(), type, java.sql.Timestamp.from(at),
                java.sql.Timestamp.from(at.plusSeconds(60)));
    }

    private AlertHistoryQuery query(Set<String> cages, Long guineaPigId, HealthStatus level, EventType type,
                                    Paging paging) {
        return new AlertHistoryQuery(cages, guineaPigId, level, type, null, WEEK, paging);
    }

    @Test
    void pagesNewestFirstWithTheTotal() {
        var first = alerts.findHistory(query(Set.of(cage), null, null, null, new Paging(0, 5)));
        var last = alerts.findHistory(query(Set.of(cage), null, null, null, new Paging(2, 5)));

        assertThat(first.totalItems()).isEqualTo(13);
        assertThat(first.items()).hasSize(5).extracting(Alert::getType).first().isEqualTo(EventType.AUDIO);
        assertThat(first.items()).isSortedAccordingTo((a, b) -> b.getCreatedAt().compareTo(a.getCreatedAt()));
        assertThat(last.items()).hasSize(3);
        assertThat(last.hasNext()).isFalse();
    }

    @Test
    void filtersByGuineaPigLevelAndTypeInTheDatabase() {
        assertThat(alerts.findHistory(query(Set.of(cage), pig, null, null, Paging.of(null, null))).totalItems())
                .isEqualTo(12);
        assertThat(alerts.findHistory(query(Set.of(cage), null, HealthStatus.CRITICAL, null, Paging.of(null, null)))
                .totalItems()).isEqualTo(3);
        assertThat(alerts.findHistory(query(Set.of(cage), null, null, EventType.AUDIO, Paging.of(null, null)))
                .items()).singleElement().extracting(Alert::getGuineaPigId).isNull();
        assertThat(alerts.findHistory(query(Set.of(cage), null, null, null, Paging.of(null, null))).items())
                .allMatch(alert -> alert.getStatus() == AlertStatus.REVIEWED && alert.getReviewedAt() != null);
    }

    @Test
    void onlyTheCagesOfTheQueryAreRead() {
        assertThat(alerts.findHistory(query(Set.of(otherCage), null, null, null, Paging.of(null, null))).totalItems())
                .isEqualTo(1);
        assertThat(alerts.findHistory(query(Set.of(cage, otherCage), null, null, null, Paging.of(null, null)))
                .totalItems()).isEqualTo(14);
    }
}
