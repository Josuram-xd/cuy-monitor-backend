package com.cuymonitor.backend.config;

import com.cuymonitor.backend.adapter.out.notification.DatabaseAlertObserver;
import com.cuymonitor.backend.adapter.out.notification.LogAlertObserver;
import com.cuymonitor.backend.adapter.out.notification.WebSocketAlertObserver;
import com.cuymonitor.backend.domain.notification.AlertPublisher;
import com.cuymonitor.backend.domain.port.out.AlertRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * <b>Observer pattern — wiring.</b> The subscribers of {@link AlertPublisher}, in the order they are told, written
 * out here instead of left to {@code @Order}: the database goes first so the alert has its id when the dashboard
 * receives it over WebSocket, and the log goes last.
 */
@Configuration
public class NotificationConfig {

    /** <b>Adapter</b> — plain Java, no {@code @Component}: it writes the alert through the repository port. */
    @Bean
    public DatabaseAlertObserver databaseAlertObserver(AlertRepository alerts) {
        return new DatabaseAlertObserver(alerts);
    }

    /** <b>Adapter</b> — Adaptee: Spring's {@code SimpMessagingTemplate}, the WebSocket this project speaks. */
    @Bean
    public WebSocketAlertObserver webSocketAlertObserver(SimpMessagingTemplate messagingTemplate) {
        return new WebSocketAlertObserver(messagingTemplate);
    }

    /** <b>Adapter</b> — Adaptee: the logger. Last subscriber, so a failure above still gets written down. */
    @Bean
    public LogAlertObserver logAlertObserver() {
        return new LogAlertObserver();
    }

    @Bean
    public AlertPublisher alertPublisher(DatabaseAlertObserver database, WebSocketAlertObserver webSocket,
                                         LogAlertObserver log) {
        return new AlertPublisher(List.of(database, webSocket, log));
    }
}
