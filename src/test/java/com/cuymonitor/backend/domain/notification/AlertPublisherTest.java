package com.cuymonitor.backend.domain.notification;

import com.cuymonitor.backend.domain.model.Alert;
import com.cuymonitor.backend.domain.model.EventType;
import com.cuymonitor.backend.domain.model.HealthStatus;
import com.cuymonitor.backend.domain.port.out.AlertObserver;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class AlertPublisherTest {

    private final Alert alert = Alert.forCage("cage-1", EventType.AUDIO, HealthStatus.ALERT, "chillidos",
            Instant.parse("2026-10-05T14:00:00Z"));

    @Test
    void notifiesEveryObserverInSubscriptionOrder() {
        List<String> calls = new ArrayList<>();
        AlertPublisher publisher = new AlertPublisher();
        publisher.subscribe(a -> calls.add("database"));
        publisher.subscribe(a -> calls.add("websocket"));
        publisher.subscribe(a -> calls.add("log"));

        publisher.publish(alert);

        assertThat(calls).containsExactly("database", "websocket", "log");
    }

    @Test
    void unsubscribedObserverIsNotNotified() {
        List<Alert> received = new ArrayList<>();
        AlertObserver observer = received::add;
        AlertPublisher publisher = new AlertPublisher();
        publisher.subscribe(observer);
        publisher.unsubscribe(observer);

        publisher.publish(alert);

        assertThat(received).isEmpty();
    }

    @Test
    void failingObserverDoesNotStopTheOthers() {
        List<Alert> received = new ArrayList<>();
        AlertPublisher publisher = new AlertPublisher();
        publisher.subscribe(a -> {
            throw new IllegalStateException("websocket down");
        });
        publisher.subscribe(received::add);

        publisher.publish(alert);

        assertThat(received).containsExactly(alert);
    }
}
