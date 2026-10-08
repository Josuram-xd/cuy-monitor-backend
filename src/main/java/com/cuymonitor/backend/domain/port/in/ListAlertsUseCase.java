package com.cuymonitor.backend.domain.port.in;

import com.cuymonitor.backend.domain.model.Alert;
import com.cuymonitor.backend.domain.model.AlertStatus;

import java.util.List;
import java.util.Optional;

public interface ListAlertsUseCase {
    List<Alert> listAlerts(Optional<AlertStatus> status);
}
