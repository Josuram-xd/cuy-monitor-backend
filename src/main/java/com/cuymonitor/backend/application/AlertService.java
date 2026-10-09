package com.cuymonitor.backend.application;

import com.cuymonitor.backend.domain.model.Alert;
import com.cuymonitor.backend.domain.model.AlertStatus;
import com.cuymonitor.backend.domain.port.in.ListAlertsUseCase;
import com.cuymonitor.backend.domain.port.out.AlertRepository;

import java.util.List;
import java.util.Optional;

public class AlertService implements ListAlertsUseCase {

    private final AlertRepository alertRepository;

    public AlertService(AlertRepository alertRepository) {
        this.alertRepository = alertRepository;
    }

    @Override
    public List<Alert> listAlerts(Optional<AlertStatus> status) {
        return status.map(alertRepository::findByStatus).orElseGet(alertRepository::findAll);
    }
}
