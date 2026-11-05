package com.cuymonitor.backend.application;

import com.cuymonitor.backend.domain.model.AlertHistoryPage;
import com.cuymonitor.backend.domain.model.AlertHistoryQuery;
import com.cuymonitor.backend.domain.port.in.GetAlertHistoryUseCase;
import com.cuymonitor.backend.domain.port.out.AlertRepository;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;

// The alert history, apart from AlertService (listing open alerts and reviewing them): one reason to change each.
public class AlertHistoryService implements GetAlertHistoryUseCase {

    private final AlertRepository alerts;

    public AlertHistoryService(AlertRepository alerts) {
        this.alerts = Objects.requireNonNull(alerts, "alerts");
    }

    @Override
    @Transactional(readOnly = true)
    public AlertHistoryPage history(AlertHistoryQuery query) {
        if (query.readsNothing()) {
            return AlertHistoryPage.empty(query.paging(), query.range());
        }
        return alerts.findHistory(query);
    }
}
