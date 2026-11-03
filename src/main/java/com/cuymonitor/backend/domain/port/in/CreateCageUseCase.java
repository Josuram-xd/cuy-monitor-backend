package com.cuymonitor.backend.domain.port.in;

import com.cuymonitor.backend.domain.model.Cage;

public interface CreateCageUseCase {
    /** The backend picks the public code of the new cage. */
    Cage create(CreateCageCommand command);
}
