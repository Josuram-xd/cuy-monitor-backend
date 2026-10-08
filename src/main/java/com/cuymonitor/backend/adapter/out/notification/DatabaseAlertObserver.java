package com.cuymonitor.backend.adapter.out.notification;

import com.cuymonitor.backend.domain.model.Alert;
import com.cuymonitor.backend.domain.port.out.AlertObserver;
import com.cuymonitor.backend.domain.port.out.AlertRepository;
import org.springframework.stereotype.Component;

// must be subscribed first: saving gives the alert the id the other observers send
@Component
public class DatabaseAlertObserver implements AlertObserver {

    private final AlertRepository alertRepository;

    public DatabaseAlertObserver(AlertRepository alertRepository) {
        this.alertRepository = alertRepository;
    }

    @Override
    public void onAlert(Alert alert) {
        alertRepository.save(alert);
    }
}
