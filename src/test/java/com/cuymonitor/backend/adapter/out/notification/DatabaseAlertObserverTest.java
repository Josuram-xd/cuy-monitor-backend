package com.cuymonitor.backend.adapter.out.notification;

import com.cuymonitor.backend.domain.model.Alert;
import com.cuymonitor.backend.domain.model.AlertStatus;
import com.cuymonitor.backend.domain.model.EventType;
import com.cuymonitor.backend.domain.model.HealthStatus;
import com.cuymonitor.backend.domain.port.out.AlertRepository;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

class DatabaseAlertObserverTest {

    @Test
    void savesThePublishedAlert() {
        var repository = new RecordingAlertRepository();
        var observer = new DatabaseAlertObserver(repository);
        var alert = Alert.forCage("cage-1", EventType.AUDIO, HealthStatus.ALERT, "noise", Instant.EPOCH);
        var savedAlert = Alert.restore(17L, "cage-1", null, HealthStatus.ALERT, EventType.AUDIO, "noise",
                AlertStatus.OPEN, Instant.EPOCH, null);
        repository.savedResult = savedAlert;

        observer.onAlert(alert);

        assertSame(alert, repository.savedAlert);
        assertEquals(17L, alert.getId());
    }

    @Test
    void rejectsNullDependenciesAndAlerts() {
        assertThrows(NullPointerException.class, () -> new DatabaseAlertObserver(null));
        assertThrows(NullPointerException.class, () -> new DatabaseAlertObserver(new RecordingAlertRepository())
                .onAlert(null));
    }

    private static final class RecordingAlertRepository implements AlertRepository {

        private Alert savedAlert;
        private Alert savedResult;

        @Override
        public Alert save(Alert alert) {
            savedAlert = alert;
            return savedResult == null ? alert : savedResult;
        }

        @Override
        public List<Alert> findAll() {
            return List.of();
        }

        @Override
        public List<Alert> findByStatus(AlertStatus status) {
            return List.of();
        }
    }
}
