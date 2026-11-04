package com.cuymonitor.backend.config;

import com.cuymonitor.backend.adapter.out.notification.DatabaseAlertObserver;
import com.cuymonitor.backend.adapter.out.notification.LogAlertObserver;
import com.cuymonitor.backend.adapter.out.notification.WebSocketAlertObserver;
import com.cuymonitor.backend.domain.notification.AlertPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * <b>Observer pattern — wiring.</b> The subscribers of {@link AlertPublisher}, in the order they are told, written
 * out here instead of left to {@code @Order}: the database goes first so the alert has its id when the dashboard
 * receives it over WebSocket, and the log goes last.
 */
@Configuration
public class NotificationConfig {

    @Bean
    public AlertPublisher alertPublisher(DatabaseAlertObserver database, WebSocketAlertObserver webSocket,
                                         LogAlertObserver log) {
        return new AlertPublisher(List.of(database, webSocket, log));
    }
}
