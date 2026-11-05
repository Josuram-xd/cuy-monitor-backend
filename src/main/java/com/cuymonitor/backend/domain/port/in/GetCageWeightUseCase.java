package com.cuymonitor.backend.domain.port.in;

import com.cuymonitor.backend.domain.model.CageWeight;
import com.cuymonitor.backend.domain.model.TimeRange;

public interface GetCageWeightUseCase {
    /** @throws com.cuymonitor.backend.domain.exception.CageNotFoundException if there is no active cage with that code */
    CageWeight weight(String cageCode, TimeRange range);
}
