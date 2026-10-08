package com.cuymonitor.backend.domain.port.in;

import com.cuymonitor.backend.domain.health.composite.CageHealth;

public interface GetCageHealthUseCase {
    CageHealth getCageHealth(String cageCode);
}
