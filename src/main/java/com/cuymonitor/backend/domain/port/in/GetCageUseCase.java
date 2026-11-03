package com.cuymonitor.backend.domain.port.in;

import com.cuymonitor.backend.domain.model.Cage;

public interface GetCageUseCase {
    /** @throws com.cuymonitor.backend.domain.exception.CageNotFoundException if there is no active cage with that code */
    Cage get(String cageCode);
}
