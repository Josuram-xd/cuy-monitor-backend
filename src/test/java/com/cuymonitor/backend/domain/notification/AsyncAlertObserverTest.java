package com.cuymonitor.backend.domain.notification;

import com.cuymonitor.backend.domain.model.Alert;
import com.cuymonitor.backend.domain.model.EventType;
import com.cuymonitor.backend.domain.model.HealthStatus;
import com.cuymonitor.backend.domain.port.out.AlertObserver;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

class AsyncAlertObserverTest {

    private static final Instant NOW = Instant.parse("2026-11-12T12:00:00Z");

    private final List<Runnable> queued = new ArrayList<>();
    private final Executor recording = queued::add;
    private final List<Alert> received = new ArrayList<>();
    private final Alert alert = Alert.forCage("cage-1", EventType.AUDIO, HealthStatus.ALERT, "ruido", NOW);

    @Test
    void theWrappedSubscriberRunsWhenTheTaskRuns() {
        var observer = new AsyncAlertObserver(received::add, recording);

        observer.onAlert(alert);

        assertThat(received).isEmpty();
        queued.forEach(Runnable::run);
        assertThat(received).containsExactly(alert);
    }

    @Test
    void aFailingSubscriberDoesNotEscapeTheTask() {
        AlertObserver failing = ignored -> {
            throw new IllegalStateException("subscriber down");
        };
        var observer = new AsyncAlertObserver(failing, recording);

        observer.onAlert(alert);

        assertThatCode(() -> queued.forEach(Runnable::run)).doesNotThrowAnyException();
    }
}
