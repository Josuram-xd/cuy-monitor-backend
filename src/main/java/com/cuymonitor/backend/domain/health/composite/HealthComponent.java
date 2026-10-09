package com.cuymonitor.backend.domain.health.composite;

import com.cuymonitor.backend.domain.model.HealthStatus;

public interface HealthComponent {

    HealthStatus status();
}
