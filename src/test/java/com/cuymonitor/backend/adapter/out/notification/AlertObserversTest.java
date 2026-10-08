package com.cuymonitor.backend.adapter.out.notification;

import com.cuymonitor.backend.adapter.out.persistence.memory.InMemoryAlertRepository;
import com.cuymonitor.backend.domain.model.Alert;
import com.cuymonitor.backend.domain.model.EventType;
import com.cuymonitor.backend.domain.model.HealthStatus;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.messaging.simp.SimpMessageSendingOperations;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class AlertObserversTest {

    private static final Instant NOW = Instant.parse("2026-10-05T14:20:00Z");

    @Test
    void databaseObserverStoresTheAlertAndGivesItAnId() {
        InMemoryAlertRepository repository = new InMemoryAlertRepository();
        Alert alert = alert();

        new DatabaseAlertObserver(repository).onAlert(alert);

        assertThat(alert.getId()).isNotNull();
        assertThat(repository.findAll()).hasSize(1);
    }

    @Test
    void webSocketObserverSendsTheAlertToTheCageTopic() {
        SimpMessageSendingOperations messaging = mock(SimpMessageSendingOperations.class);
        Alert alert = alert();
        alert.assignId(7);

        new WebSocketAlertObserver(messaging).onAlert(alert);

        ArgumentCaptor<CageTopicMessage> sent = ArgumentCaptor.forClass(CageTopicMessage.class);
        verify(messaging).convertAndSend(eq("/topic/cages/cage-1"), sent.capture());
        assertThat(sent.getValue().type()).isEqualTo("ALERT");
        assertThat(sent.getValue().cageId()).isEqualTo("cage-1");
        assertThat(sent.getValue().occurredAt()).isEqualTo(NOW);
        AlertMessage data = (AlertMessage) sent.getValue().data();
        assertThat(data.id()).isEqualTo(7);
        assertThat(data.level()).isEqualTo(HealthStatus.CRITICAL);
        assertThat(data.guineaPigId()).isNull();
    }

    @Test
    void logObserverNeverFails() {
        assertThatCode(() -> new LogAlertObserver().onAlert(alert())).doesNotThrowAnyException();
    }

    private static Alert alert() {
        return Alert.forCage("cage-1", EventType.AUDIO, HealthStatus.CRITICAL, "chillidos de angustia", NOW);
    }
}
