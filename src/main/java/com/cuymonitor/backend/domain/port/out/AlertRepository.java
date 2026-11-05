package com.cuymonitor.backend.domain.port.out;

import com.cuymonitor.backend.domain.model.Alert;
import com.cuymonitor.backend.domain.model.AlertHistoryPage;
import com.cuymonitor.backend.domain.model.AlertHistoryQuery;
import com.cuymonitor.backend.domain.model.AlertStatus;

import java.util.List;
import java.util.Optional;

public interface AlertRepository {
    Alert save(Alert alert);
    Optional<Alert> findById(long id);
    // newest first
    List<Alert> findAll();
    List<Alert> findByStatus(AlertStatus status);
    // one page, newest first, filtered in the database and not in memory
    AlertHistoryPage findHistory(AlertHistoryQuery query);
}
