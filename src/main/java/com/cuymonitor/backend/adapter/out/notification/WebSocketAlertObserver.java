package com.cuymonitor.backend.adapter.out.notification;

import com.cuymonitor.backend.domain.model.Alert;
import com.cuymonitor.backend.domain.port.out.AlertObserver;
import java.util.Objects;
import org.springframework.messaging.simp.SimpMessagingTemplate;

public final class WebSocketAlertObserver implements AlertObserver {

    private final SimpMessagingTemplate messagingTemplate;

    public WebSocketAlertObserver(SimpMessagingTemplate messagingTemplate) {
        this.messagingTemplate = Objects.requireNonNull(messagingTemplate, "messagingTemplate");
    }

    @Override
    public void onAlert(Alert alert) {
        var nonNullAlert = Objects.requireNonNull(alert, "alert");
        var data = new AlertWebSocketMessage.Data(
                nonNullAlert.getId(),
                nonNullAlert.getCageCode(),
                nonNullAlert.getGuineaPigId(),
                nonNullAlert.getLevel(),
                nonNullAlert.getType(),
                nonNullAlert.getMessage(),
                nonNullAlert.getStatus(),
                nonNullAlert.getCreatedAt(),
                nonNullAlert.getReviewedAt(),
                nonNullAlert.getAiNote(),
                nonNullAlert.getAiNoteAt()
        );
        var message = new AlertWebSocketMessage(
                "ALERT",
                nonNullAlert.getCageCode(),
                nonNullAlert.getCreatedAt(),
                data
        );
        messagingTemplate.convertAndSend("/topic/cages/" + nonNullAlert.getCageCode(), message);
    }
}
