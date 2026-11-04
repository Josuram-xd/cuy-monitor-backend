package com.cuymonitor.backend.adapter.out.notification;

import com.cuymonitor.backend.domain.model.Alert;
import com.cuymonitor.backend.domain.port.out.AlertObserver;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public final class LogAlertObserver implements AlertObserver {

    private static final Logger log = LoggerFactory.getLogger(LogAlertObserver.class);

    @Override
    public void onAlert(Alert alert) {
        Objects.requireNonNull(alert, "alert");
        log.warn("Health alert: cage={}, guineaPigId={}, level={}, type={}, message={}",
                alert.getCageCode(), alert.getGuineaPigId(), alert.getLevel(), alert.getType(), alert.getMessage());
    }
}
