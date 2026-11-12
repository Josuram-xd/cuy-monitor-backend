package com.cuymonitor.backend.config;

import com.cuymonitor.backend.adapter.out.notification.DatabaseAlertObserver;
import com.cuymonitor.backend.adapter.out.notification.LogAlertObserver;
import com.cuymonitor.backend.adapter.out.notification.WebSocketAlertObserver;
import com.cuymonitor.backend.application.AlertNoteService;
import com.cuymonitor.backend.domain.notification.AlertPublisher;
import com.cuymonitor.backend.domain.port.out.AlertRepository;
import com.cuymonitor.backend.domain.port.out.AssistantModel;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * <b>Observer pattern — wiring.</b> The subscribers of {@link AlertPublisher}, in the order they are told, written
 * out here instead of left to {@code @Order}: the database goes first so the alert has its id when the dashboard
 * receives it over WebSocket, the assistant's note is asked for after the dashboard already has the alert, and
 * the log goes last.
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

    /**
     * The pool the assistant's note is written on. Small and bounded on purpose: these calls cost money and the
     * alerts of one small farm arrive minutes apart, so two threads are plenty. If a burst ever fills the queue,
     * CallerRunsPolicy would block the ingestion request, so the note is dropped instead — the alert itself is
     * already stored and sent, and an alert without a note is worth more than a slow camera.
     */
    @Bean(destroyMethod = "shutdown")
    public ThreadPoolExecutor alertNoteExecutor() {
        var executor = new ThreadPoolExecutor(1, 2, 60L, TimeUnit.SECONDS, new LinkedBlockingQueue<>(50),
                runnable -> {
                    Thread thread = new Thread(runnable, "alert-note");
                    thread.setDaemon(true);
                    return thread;
                },
                new ThreadPoolExecutor.DiscardPolicy());
        executor.allowCoreThreadTimeOut(true);
        return executor;
    }

    /**
     * <b>Observer pattern — subscriber.</b> Asks the model to write about the alert, off the request thread. It
     * gets the WebSocket subscriber so it can send the alert again when the note is ready; going through
     * {@link AlertPublisher} instead would call this subscriber again, and again.
     */
    @Bean
    public AlertNoteService alertNoteService(AssistantModel model, AlertRepository alerts,
                                            WebSocketAlertObserver webSocket, Clock clock,
                                            Executor alertNoteExecutor,
                                            @Value("${app.assistant.alert-note-language:es}") String language) {
        return new AlertNoteService(model, alerts, webSocket, clock, alertNoteExecutor, language);
    }

    @Bean
    public AlertPublisher alertPublisher(DatabaseAlertObserver database, WebSocketAlertObserver webSocket,
                                         AlertNoteService note, LogAlertObserver log) {
        return new AlertPublisher(List.of(database, webSocket, note, log));
    }
}
