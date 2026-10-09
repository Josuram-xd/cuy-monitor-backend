package com.cuymonitor.backend.domain.notification;

import com.cuymonitor.backend.domain.model.Alert;
import com.cuymonitor.backend.domain.port.out.AlertObserver;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;

public class AlertPublisher {

    private static final System.Logger log = System.getLogger(AlertPublisher.class.getName());

    private final List<AlertObserver> observers = new CopyOnWriteArrayList<>();

    public void subscribe(AlertObserver observer) {
        observers.add(Objects.requireNonNull(observer, "observer"));
    }

    public void unsubscribe(AlertObserver observer) {
        observers.remove(observer);
    }

    // observers are called in the order they subscribed; one failing must not stop the others
    public void publish(Alert alert) {
        Objects.requireNonNull(alert, "alert");
        for (AlertObserver observer : observers) {
            try {
                observer.onAlert(alert);
            } catch (RuntimeException ex) {
                log.log(System.Logger.Level.ERROR, "observer " + observer.getClass().getSimpleName() + " failed", ex);
            }
        }
    }

    public List<AlertObserver> observers() {
        return List.copyOf(observers);
    }
}
