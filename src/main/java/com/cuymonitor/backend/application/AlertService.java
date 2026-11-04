package com.cuymonitor.backend.application;

import com.cuymonitor.backend.domain.exception.AlertNotFoundException;
import com.cuymonitor.backend.domain.model.Alert;
import com.cuymonitor.backend.domain.model.AlertStatus;
import com.cuymonitor.backend.domain.port.in.ListAlertsUseCase;
import com.cuymonitor.backend.domain.port.in.ReviewAlertUseCase;
import com.cuymonitor.backend.domain.port.out.AlertRepository;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.util.List;

public class AlertService implements ListAlertsUseCase, ReviewAlertUseCase {

    private final AlertRepository alerts;
    private final Clock clock;

    public AlertService(AlertRepository alerts, Clock clock) {
        this.alerts = alerts;
        this.clock = clock;
    }

    @Override
    @Transactional(readOnly = true)
    public List<Alert> list(AlertStatus status) {
        return status == null ? alerts.findAll() : alerts.findByStatus(status);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Alert> listForCage(String cageCode, AlertStatus status) {
        return list(status).stream().filter(alert -> alert.belongsTo(cageCode)).toList();
    }

    @Override
    @Transactional
    public Alert markReviewed(long alertId) {
        Alert alert = alerts.findById(alertId).orElseThrow(AlertNotFoundException::new);
        if (!alert.isOpen()) {
            return alert;
        }
        alert.markReviewed(clock.instant());
        return alerts.save(alert);
    }
}
