package com.cuymonitor.backend.domain.port.out;

import com.cuymonitor.backend.domain.model.Alert;

@FunctionalInterface
public interface AlertObserver {

    void onAlert(Alert alert);
}
