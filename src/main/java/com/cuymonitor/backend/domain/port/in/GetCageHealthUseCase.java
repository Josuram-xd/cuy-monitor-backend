package com.cuymonitor.backend.domain.port.in;

import com.cuymonitor.backend.domain.model.CageHealthSummary;

public interface GetCageHealthUseCase {
    CageHealthSummary getHealth(String cageCode);
}
