package com.cuymonitor.backend.domain.port.in;

import com.cuymonitor.backend.domain.model.Cage;

public interface UpdateCageUseCase {
    Cage update(UpdateCageCommand command);
}
