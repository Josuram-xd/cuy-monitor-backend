package com.cuymonitor.backend.domain.notification;

import com.cuymonitor.backend.domain.model.Alert;
import com.cuymonitor.backend.domain.port.out.AlertObserver;
import java.util.List;
import java.util.Objects;

public final class AlertPublisher {

    private final List<AlertObserver> observers;

    public AlertPublisher(List<AlertObserver> observers) {
        this.observers = List.copyOf(Objects.requireNonNull(observers, "observers"));
    }

    public void publish(Alert alert) {
        Objects.requireNonNull(alert, "alert");
        observers.forEach(observer -> observer.onAlert(alert));
    }
}
