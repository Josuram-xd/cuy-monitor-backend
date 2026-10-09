package com.cuymonitor.backend.domain.port.in;

import com.cuymonitor.backend.domain.model.GuineaPig;

import java.util.List;

public interface ListGuineaPigsUseCase {
    /** The active guinea pigs of the cage, oldest first. */
    List<GuineaPig> list(String cageCode);
}
