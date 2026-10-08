package com.cuymonitor.backend.adapter.out.notification;

import com.cuymonitor.backend.domain.model.Alert;
import com.cuymonitor.backend.domain.port.out.AlertObserver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class LogAlertObserver implements AlertObserver {

    private static final Logger log = LoggerFactory.getLogger(LogAlertObserver.class);

    @Override
    public void onAlert(Alert alert) {
        log.warn("Alert id={} cage={} guineaPig={} level={} type={}: {}", alert.getId(), alert.getCageCode(),
                alert.getGuineaPigId(), alert.getLevel(), alert.getType(), alert.getMessage());
    }
}
