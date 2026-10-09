package com.cuymonitor.backend.domain.port.in;

import com.cuymonitor.backend.domain.model.GuineaPig;

public interface RegisterGuineaPigUseCase {
    GuineaPig register(RegisterGuineaPigCommand registerGuineaPigCommand);
}
