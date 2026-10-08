package com.cuymonitor.backend.domain.port.in;

import com.cuymonitor.backend.domain.model.GuineaPig;

import java.util.List;

public interface ListGuineaPigsUseCase {
    List<GuineaPig> listGuineaPigs(String cageCode);
}
