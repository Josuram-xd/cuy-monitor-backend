package com.cuymonitor.backend.adapter.out.notification;

import com.cuymonitor.backend.domain.model.Alert;
import com.cuymonitor.backend.domain.model.AlertStatus;
import com.cuymonitor.backend.domain.model.EventType;
import com.cuymonitor.backend.domain.model.HealthStatus;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;

class WebSocketAlertObserverTest {

    @Test
    void publishesAnAlertEnvelopeToItsCageTopic() {
        var messagingTemplate = mock(SimpMessagingTemplate.class);
        var observer = new WebSocketAlertObserver(messagingTemplate);
        var createdAt = Instant.parse("2026-10-05T14:20:00Z");
        var alert = Alert.restore(7L, "cage-1", 3L, HealthStatus.CRITICAL, EventType.BEHAVIOR,
                "Canela is unwell", AlertStatus.OPEN, createdAt, null);

        observer.onAlert(alert);

        var messageCaptor = ArgumentCaptor.forClass(AlertWebSocketMessage.class);
        verify(messagingTemplate).convertAndSend(eq("/topic/cages/cage-1"), messageCaptor.capture());
        var message = messageCaptor.getValue();
        assertEquals("ALERT", message.type());
        assertEquals("cage-1", message.cageId());
        assertEquals(createdAt, message.occurredAt());
        assertEquals(new AlertWebSocketMessage.Data(7L, "cage-1", 3L, HealthStatus.CRITICAL, EventType.BEHAVIOR,
                "Canela is unwell", AlertStatus.OPEN, createdAt, null), message.data());
    }

    @Test
    void rejectsNullDependenciesAndAlerts() {
        assertThrows(NullPointerException.class, () -> new WebSocketAlertObserver(null));

        var messagingTemplate = mock(SimpMessagingTemplate.class);
        var observer = new WebSocketAlertObserver(messagingTemplate);
        assertThrows(NullPointerException.class, () -> observer.onAlert(null));
        verifyNoInteractions(messagingTemplate);
    }
}
