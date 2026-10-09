package com.cuymonitor.backend.adapter.out.notification;

import com.cuymonitor.backend.domain.model.Alert;
import com.cuymonitor.backend.domain.port.out.AlertObserver;
import com.cuymonitor.backend.domain.port.out.AlertRepository;
import java.util.Objects;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public final class DatabaseAlertObserver implements AlertObserver {

    private final AlertRepository alertRepository;

    public DatabaseAlertObserver(AlertRepository alertRepository) {
        this.alertRepository = Objects.requireNonNull(alertRepository, "alertRepository");
    }

    @Override
    public void onAlert(Alert alert) {
        var nonNullAlert = Objects.requireNonNull(alert, "alert");
        var savedAlert = Objects.requireNonNull(alertRepository.save(nonNullAlert), "savedAlert");
        if (savedAlert.getId() != null) {
            nonNullAlert.assignId(savedAlert.getId());
        }
    }
}
