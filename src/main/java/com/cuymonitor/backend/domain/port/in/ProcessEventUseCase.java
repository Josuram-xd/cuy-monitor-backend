package com.cuymonitor.backend.domain.port.in;

import com.cuymonitor.backend.domain.model.HealthEvent;

public interface ProcessEventUseCase {
    void process(HealthEvent event);
}
