package com.cuymonitor.backend.domain.port.in;

import com.cuymonitor.backend.domain.model.AlertHistoryPage;
import com.cuymonitor.backend.domain.model.AlertHistoryQuery;

public interface GetAlertHistoryUseCase {

    AlertHistoryPage history(AlertHistoryQuery query);
}
