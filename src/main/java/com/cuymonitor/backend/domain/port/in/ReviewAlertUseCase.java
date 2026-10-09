package com.cuymonitor.backend.domain.port.in;

import com.cuymonitor.backend.domain.model.Alert;

public interface ReviewAlertUseCase {
    /** Marking an alert that is already reviewed changes nothing and is not an error. */
    Alert markReviewed(long alertId);
}
