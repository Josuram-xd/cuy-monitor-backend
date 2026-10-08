package com.cuymonitor.backend.application;

import com.cuymonitor.backend.domain.health.fake.HealthFakes;
import com.cuymonitor.backend.domain.model.Alert;
import com.cuymonitor.backend.domain.model.AlertStatus;
import com.cuymonitor.backend.domain.model.EventType;
import com.cuymonitor.backend.domain.model.HealthStatus;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class AlertServiceTest {

    private static final Instant NOW = Instant.parse("2026-10-05T14:20:00Z");

    private final HealthFakes.Alerts alerts = new HealthFakes.Alerts();
    private final AlertService service = new AlertService(alerts);

    @Test
    void filtersByStatusAndListsNewestFirst() {
        Alert first = alerts.save(Alert.forCage("cage-1", EventType.AUDIO, HealthStatus.ALERT, "uno", NOW));
        Alert second = alerts.save(Alert.forCage("cage-1", EventType.AUDIO, HealthStatus.ALERT, "dos",
                NOW.plusSeconds(60)));
        first.markReviewed(NOW.plusSeconds(120));

        assertThat(service.listAlerts(Optional.empty())).containsExactly(second, first);
        assertThat(service.listAlerts(Optional.of(AlertStatus.OPEN))).containsExactly(second);
        assertThat(service.listAlerts(Optional.of(AlertStatus.REVIEWED))).containsExactly(first);
    }
}
