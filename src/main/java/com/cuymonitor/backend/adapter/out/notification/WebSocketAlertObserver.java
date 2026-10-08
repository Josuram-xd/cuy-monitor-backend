package com.cuymonitor.backend.adapter.out.notification;

import com.cuymonitor.backend.domain.model.Alert;
import com.cuymonitor.backend.domain.port.out.AlertObserver;
import org.springframework.messaging.simp.SimpMessageSendingOperations;
import org.springframework.stereotype.Component;

@Component
public class WebSocketAlertObserver implements AlertObserver {

    private final SimpMessageSendingOperations messaging;

    public WebSocketAlertObserver(SimpMessageSendingOperations messaging) {
        this.messaging = messaging;
    }

    @Override
    public void onAlert(Alert alert) {
        CageTopicMessage message = new CageTopicMessage("ALERT", alert.getCageCode(), alert.getCreatedAt(),
                AlertMessage.from(alert));
        messaging.convertAndSend("/topic/cages/" + alert.getCageCode(), message);
    }
}
