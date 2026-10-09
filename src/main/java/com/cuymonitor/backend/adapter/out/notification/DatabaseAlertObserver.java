package com.cuymonitor.backend.adapter.out.notification;

import com.cuymonitor.backend.domain.model.Alert;
import com.cuymonitor.backend.domain.port.out.AlertObserver;
import com.cuymonitor.backend.domain.port.out.AlertRepository;
import java.util.Objects;
import org.springframework.stereotype.Component;

@Component
public final class DatabaseAlertObserver implements AlertObserver {

    private final AlertRepository alertRepository;

    public DatabaseAlertObserver(AlertRepository alertRepository) {
        this.alertRepository = Objects.requireNonNull(alertRepository, "alertRepository");
    }

    @Override
    public void onAlert(Alert alert) {
        alertRepository.save(Objects.requireNonNull(alert, "alert"));
    }
}
