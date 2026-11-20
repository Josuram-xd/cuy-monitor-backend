package com.cuymonitor.backend.domain.notification;

import com.cuymonitor.backend.domain.model.Alert;
import com.cuymonitor.backend.domain.port.out.AlertObserver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;
import java.util.concurrent.Executor;

/**
 * <b>Decorator.</b> Same AlertObserver interface; runs the wrapped subscriber on an executor so a slow one never
 * holds the ingestion request.
 */
public final class AsyncAlertObserver implements AlertObserver {

    private static final Logger log = LoggerFactory.getLogger(AsyncAlertObserver.class);

    private final AlertObserver inner;
    private final Executor executor;

    public AsyncAlertObserver(AlertObserver inner, Executor executor) {
        this.inner = Objects.requireNonNull(inner, "inner");
        this.executor = Objects.requireNonNull(executor, "executor");
    }

    @Override
    public void onAlert(Alert alert) {
        Objects.requireNonNull(alert, "alert");
        executor.execute(() -> {
            try {
                inner.onAlert(alert);
            } catch (RuntimeException ex) {
                // runs on a pool thread: nobody else would see the failure
                log.error("async alert subscriber failed for alert {}", alert.getId(), ex);
            }
        });
    }
}
