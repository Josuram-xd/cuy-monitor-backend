package com.cuymonitor.backend.domain.port.in;

import com.cuymonitor.backend.domain.model.Alert;
import com.cuymonitor.backend.domain.model.AlertStatus;

import java.util.List;

public interface ListAlertsUseCase {
    /** Newest first. A null status means every alert. */
    List<Alert> list(AlertStatus status);
}
