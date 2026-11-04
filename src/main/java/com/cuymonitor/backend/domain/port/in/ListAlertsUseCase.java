package com.cuymonitor.backend.domain.port.in;

import com.cuymonitor.backend.domain.model.Alert;
import com.cuymonitor.backend.domain.model.AlertStatus;

import java.util.List;

public interface ListAlertsUseCase {
    /** Newest first. A null status means every alert. */
    List<Alert> list(AlertStatus status);

    /** Newest first, only the alerts of one cage. A null status means every alert of it. */
    List<Alert> listForCage(String cageCode, AlertStatus status);
}
