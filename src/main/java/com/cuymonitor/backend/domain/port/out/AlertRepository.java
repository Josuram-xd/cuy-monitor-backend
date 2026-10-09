package com.cuymonitor.backend.domain.port.out;

import com.cuymonitor.backend.domain.model.Alert;
import com.cuymonitor.backend.domain.model.AlertStatus;

import java.util.List;

public interface AlertRepository {
    Alert save(Alert alert);
    // newest first
    List<Alert> findAll();
    List<Alert> findByStatus(AlertStatus status);
}
