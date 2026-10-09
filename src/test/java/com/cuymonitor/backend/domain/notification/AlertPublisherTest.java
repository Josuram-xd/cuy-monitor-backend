package com.cuymonitor.backend.domain.notification;

import com.cuymonitor.backend.domain.model.Alert;
import com.cuymonitor.backend.domain.model.EventType;
import com.cuymonitor.backend.domain.model.HealthStatus;
import com.cuymonitor.backend.domain.port.out.AlertObserver;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

class AlertPublisherTest {

    @Test
    void publishesTheAlertToEveryObserverInRegistrationOrder() {
        var received = new ArrayList<String>();
        AlertObserver first = alert -> received.add("first");
        AlertObserver second = alert -> received.add("second");
        var publisher = new AlertPublisher(List.of(first, second));
        var alert = alert();

        publisher.publish(alert);

        assertEquals(List.of("first", "second"), received);
    }

    @Test
    void doesNothingWhenThereAreNoObservers() {
        new AlertPublisher(List.of()).publish(alert());
    }

    @Test
    void rejectsNullObserverListEntriesAndAlerts() {
        assertThrows(NullPointerException.class, () -> new AlertPublisher(null));
        assertThrows(NullPointerException.class, () -> new AlertPublisher(Arrays.asList((AlertObserver) null)));
        assertThrows(NullPointerException.class, () -> new AlertPublisher(List.of()).publish(null));
    }

    @Test
    void deliversTheSameAlertInstance() {
        var published = new ArrayList<Alert>();
        var alert = alert();
        var publisher = new AlertPublisher(List.of(published::add));

        publisher.publish(alert);

        assertSame(alert, published.get(0));
    }

    private static Alert alert() {
        return Alert.forCage("cage-1", EventType.AUDIO, HealthStatus.ALERT, "noisy", Instant.EPOCH);
    }
}
